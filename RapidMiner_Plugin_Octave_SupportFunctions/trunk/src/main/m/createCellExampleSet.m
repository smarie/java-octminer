%% function [CellExampleSet]=createCellExampleSet(levelEncoded_data, name, role, level)
%
% This function creates a cell exampleset compliant with the Octave
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
%          CellExampleSet : a {3xN} cell.
%                Row 1:  attribute names: each of the N entries is a string
%                Row 2:  attribute roles: each of the N entries is a string
%                        or empty.
%                Row 3:  attribute data: each entry is a Mx1 array or a Mx1
%                        cell. In such case this means that the attribute
%                        is nominal
%
%
% Authors: Yaoyu Zhang, Sylvain Marié
% Last modified: 23/07/12
%%
function CellExampleSet= createCellExampleSet(levelEncoded_data, name, role, level)

[nbExamples nbAttributes] = size(levelEncoded_data);
CellExampleSet=cell(3,nbAttributes);

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

% reuse the same input validation than createStructExampleSet
s = createStructExampleSet(levelEncoded_data, name, role, level);


%% 2. Cell creation
CellExampleSet(1,:)=s.name;
CellExampleSet(2,:)=s.role;

for i=1:nbAttributes    
    if  cellfun ( @isempty, (level(1,i)))== 0
        % use the provided dictionary to create the data
        CellExampleSet{3,i}=level{1,i}(levelEncoded_data(:,i));
    else
        % use the data directly
        CellExampleSet{3,i}=levelEncoded_data(:,i);
    end
end


end
