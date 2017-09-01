function [data name label]= getDataFromStruct(StructData)

data = StructData.data{1};


name = StructData.name{1}(1:end);

label = StructData.role{1}(1:end);

end