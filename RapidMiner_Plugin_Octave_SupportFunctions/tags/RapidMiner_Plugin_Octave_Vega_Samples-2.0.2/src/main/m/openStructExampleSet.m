%% function [levelEncoded_data name role level]=openStructExampleSet(structExampleSet)
% 
% This function extracts the data from the given struct generated by Octave
% extension for Rapidminer. It also checks that the struct is well-formed.
% Below we suppose that the ExampleSet has M examples and N attributes.
%
% INPUTS:
%           structExampleSet : a struct with the following fields:
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
% OUTPUTS:
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
%
% Authors: Yaoyu Zhang, Sylvain Mari�
% Last modified: 23/07/12
%% 
function [levelEncoded_data name role level]=openStructExampleSet(structExampleSet)
	
	%% extract useful fields
    if(~isfield(structExampleSet, 'name'));
        error('The provided struct should contain a "name" field');
    else
        name = structExampleSet.name;
    end
    if(~isfield(structExampleSet, 'role'));
        error('The provided struct should contain a "role" field');
    else
        role = structExampleSet.role;
    end
    if(~isfield(structExampleSet, 'data'));
        error('The provided struct should contain a "data" field');
    else
        levelEncoded_data = structExampleSet.data;
    end
    if(isfield(structExampleSet, 'levelname'));
        level = structExampleSet.levelname;
    else
        level = NaN;
    end
    
    %% reuse the same input validation than createStructExampleSet
    createStructExampleSet(levelEncoded_data, name, role, level);	
	
end