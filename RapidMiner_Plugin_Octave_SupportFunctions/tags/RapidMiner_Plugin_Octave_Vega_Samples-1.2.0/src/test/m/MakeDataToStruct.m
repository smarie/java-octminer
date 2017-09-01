function StructData= MakeDataToStruct(data)

[nbData nbDim] = size(data);

StructData.name=cell(1,nbDim);
StructData.role=cell(1,nbDim);

%StructData.name{1}="a";
%StructData.name{2}="b";
%StructData.name{3}="c";
%StructData.name{4}="d";
%StructData.name{5}="e";


	
for i=1:nbDim
	attName= [ 'att' num2str(i)];

	StructData.name{i}=attName;
	
end

for i=1:nbDim
	StructData.role{i}="";
end


StructData.data={data};

end
