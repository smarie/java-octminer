function StructData= MakeDataToStructFin(data,name,role)

[nbData nbDim] = size(data);

%StructData.name{1}{1}="a";
%StructData.name{1}{2}="b";
%StructData.name{1}{3}="c";
%StructData.name{1}{4}="d";
%StructData.name{1}{5}="e";

StructData.name{1}=name;
StructData.role{1}=role;
StructData.data(1)={data};

end

