%% function [StructExampleSet]=createStructExampleSet(levelEncoded_data, name, role, level)
%
% This function creates a struct exampleset compliant with the Octave
% extension for Rapidminer, using the provided input arguments to define
% the contents.
% Below we suppose that the ExampleSet has M examples and N attributes.
%
% INPUTS:
%          name:
%                   a 1xN cell containing the attribute names
%          role:
%                   a 1xN cell containing the attribute roles (such as
%                   'regular', 'weight', 'label', etc.). Might be empty.
%
%          levelEncoded_data:
%                   a MxN array containing the whole example set with all
%                   attributes encoded as numeric values. Numeric
%                   attributes' contents are taken as is, whereas for
%                   nominal attributes a dictionary of possible levels is
%                   created automatically and stored in 'level'. In such
%                   case the corresponding column in levelEncoded_data
%                   contains the indices of the attribute values in the
%                   corresponding dictionary
%          level:
%                   a 1xN cell containing the dictionary created for the
%                   nominal attributes. The dictionary may be empty (for
%                   numeric attributes), or may be a cell (general case
%                   where the levels are strings arrays), or may be an
%                   array (for nominal attributes with numeric levels).
%
% OUTPUTS:
%          StructExampleSet : a struct with the following fields:
%                name: a 1xN cell containing the attribute names. Each of
%                      the N entries is a string
%                role: a 1xN cell containing the attribute roles. Each of
%                      the N entries is a string or empty
%                data: a MxN array. Columns for nominal attributes contain
%                      an encoded version of the nominal data, using the
%                      dictionary provided in the 'level' field.
%                levelname: a 1xN cell containing the dictionary for the
%                      nominal attributes. The dictionary may be empty
%                      (for numeric attributes), or may be a cell (general
%                      case where the levels are strings arrays), or may be
%                      an array (for nominal attributes with numeric
%                      levels).
%
% Authors: Yaoyu Zhang, Sylvain Marié
% Last modified: 23/07/12
%%
function StructData= createStructExampleSet(levelEncoded_data, name, role, level)

[nbExamples nbAttributes] = size(levelEncoded_data);

%% 1. Input validation
% varargin can contain (name ,role ,level)
if nargin < 2 ;
    name=NaN;
end
if nargin < 3 ;
    role=NaN;
end
if nargin < 4 ;
    level=NaN;
end

% validate names
if(isnumeric(name) && isnan(name));
    disp('No attribute names available. Generating them using att-i.');
    name=cell(1,nbAttributes);
    for i=1:nbAttributes
        name{1,i}=[ 'att' num2str(i)];
    end
elseif(~((size(name,1)==1)&&(size(name,2)==nbAttributes)));
    error(['You should provide as many names as attributes. Found ' size(name,2) ' names whereas data provides ' nbAttributes ' attributes(columns)']);
end

% validate roles
if(isnumeric(role) && isnan(role));
    disp('Information: no roles attribute available.');
    role=cell(1,nbAttributes);
elseif(~((size(role,1)==1)&&(size(role,2)==nbAttributes)));
    error(['You should provide as many roles as attributes. Found ' size(role,2) ' roles whereas data provides ' nbAttributes ' attributes(columns)']);
end

% validate level for nominal attributes
if(isnumeric(level) && isnan(level));
    disp('No nominal attribute - considering all numerical');
    level=cell(1,nbAttributes);   
    level(:,:)={''}; % this is not necessary but cleaner
elseif(~((size(level,1)==1)&&(size(level,2)==nbAttributes)));
    error(['You should provide as many levels as attributes. Found ' size(level,2) ' levels whereas data provides ' nbAttributes ' attributes(columns)']);
end


%% 2. Struct creation
StructData.name=name;
StructData.role=role;
StructData.data=levelEncoded_data;
StructData.levelname=level;

end