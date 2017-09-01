function StructData= MakeDataToStruct(data)

[nbData nbDim] = size(data);

StructData.name{1}=cell(1,nbDim);
StructData.role{1}=cell(1,nbDim);

%StructData.name{1}{1}="a";
%StructData.name{1}{2}="b";
%StructData.name{1}{3}="c";
%StructData.name{1}{4}="d";
%StructData.name{1}{5}="e";


	
for i=1:nbDim
	attName= [ 'att' num2str(i)];

	StructData.name{1}{i}=attName;
	
end

for i=1:nbDim
	StructData.role{1}{i}="R";
end


StructData.data(1)={data};

end
