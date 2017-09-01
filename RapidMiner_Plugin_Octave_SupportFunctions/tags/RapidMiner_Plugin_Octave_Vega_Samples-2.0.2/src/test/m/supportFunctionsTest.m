% LOAD the data, a variable named 'In'
load('..\resources\M-2-bin.mat')

% TEST-1: open and recreate it as cell
% ------------------------------------
disp('*** Test 1: open RM-generated cell and recreate one');
[levelEncoded_data, name, role, level]=openExampleSet(In);
In2 = createCellExampleSet(levelEncoded_data, name, role, level);

if(~compareExampleSets(In,In2));
	error('Test 1 failed');
else
    disp('*** Test 1 passed');
end


% TEST-2: open and recreate it as struct
% -------------------------------------
disp('*** Test 2: open RM-generated cell and transform it to a struct');
[levelEncoded_data, name, role, level]=openExampleSet(In);
In3 = createStructExampleSet(levelEncoded_data, name, role, level);

if(~compareExampleSets(In,In3));
	error('Test 2 failed');
else
    disp('*** Test 2 passed');
end

% TEST-3: open this struct and recreate a cell
% --------------------------------------------
disp('*** Test 3: recreate a cell from this struct');
[levelEncoded_data, name, role, level]=openExampleSet(In3);
In4 = createCellExampleSet(levelEncoded_data, name, role, level);

if(~compareExampleSets(In,In4));
	error('Test 3 failed');
else
    disp('*** Test 3 passed');
end
