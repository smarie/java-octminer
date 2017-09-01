function StructData= MakeDataToStruct(data , varargin)
	%%%%%%%%%%%%%%%%%
	%
	% varargin is input var dynamic , it have 3 type possibly
	% 1, name
	% 2, name ,role 
	% 3, name ,role ,levelname (if the role are not empty)
	% name : 1xN cell
	% role : 1xN cell
	% levelname : M x num (num is numebre of role not empty)
	% data : MxN matrix
	% the field of StructData,
	% StructData.name, StructData.role,StructData.data and StructData.levelname 
	% if is exist special attribut (levelname not empty)
	% so data size is  M x N+1 , 1 column add for Rapidminer know special attribut in ExemplSet
	% 
	%%%%%%%%%%%%%%%%%
	[nbData nbDim] = size(data);
	n=length(varargin);
	if n==0 %no name, on role input 
		disp("no name, role input ")
		StructData.name=cell(1,nbDim);
		StructData.role=cell(1,nbDim);
		for i=1:nbDim
			attName= [ 'att' num2str(i)];
			StructData.name{i}=attName;
		end
		for i=1:nbDim
			StructData.role{i}="";
		end
		StructData.data={data};
	elseif n==1 % only name input 
		disp("only name input ")
		name=varargin{1};
		StructData.name=name;
		for i=1:nbDim
			StructData.role{i}="";
		end
		StructData.data={data};
	elseif n==2 % name and role input 
		disp("name and role input ")
		name=varargin{1};
		StructData.name=name;
		role=varargin{2};
		StructData.role=role;
		StructData.data={data};
	elseif n==3 % name and role ,levelname input 
		disp("name and role ,levelname input")
		
		name=varargin{1};
	
		if size(name,1)==1
			StructData.name=name;
		else
			namecopy=cell();
			for i=1:size(name,1)
				namecopy{1,i}=name{i};
			end
			StructData.name=namecopy;
		end
		role=varargin{2};
		if size(role,1)==1
			StructData.role=role;
		else
			rolecopy=cell();
			for i=1:size(role,1)
				rolecopy{1,i}=role{i};
			end
			StructData.role=rolecopy;
		end
		levelname=varargin{3};
		if size(data,2) ~= size(role,1) |  size(data,2) ~= size(role,2)
			coladd=[];
			
			idattribut=ones(size(data,1),size(levelname,2)); % for one special attribute
			coladd=[coladd idattribut*999];
			data=[data coladd];
			StructData.data={data};
		else
			StructData.data={data};
		end
		
		StructData.levelname=levelname;
		
	else
		disp("Error: type: Output arguments must be 3 or 4 for level values")
	end
end