%% function [equals]=compareExampleSets(ex1,ex2)
%
%  A function that returns 1 if both example sets are identical
%
% Authors: Yaoyu Zhang, Sylvain Marié
% Last modified: 23/07/12
%% 
function [equals]=compareExampleSets(ex1,ex2)
 
[levelEncoded_data1 name1 role1 level1] = openExampleSet(ex1);
[levelEncoded_data2 name2 role2 level2] = openExampleSet(ex2);

equals= isequal(levelEncoded_data1,levelEncoded_data2) && ...
        isequal(name1,name2) && ...
        isequal(role1,role2) && ...
        isequal(level1,level2);

end