function StructData= MakeDataToStruct(data , varargin)
	%%%%%%%%%%%%%%%%%
	%
	% varargin is input var dynamic , it have 3 type possibly
	% 1, name
	% 2, name ,role 
	% 3, name ,role ,levelname (if the role are not empty)
	% name : 1xN cell
	% role : 1xN cell
	% levelname : M x N (num is numebre of role not empty)
	% if role is empty the levelname is empty;
	% else the levelname is the value of special attribut
	% data : MxN matrix complet (with the index of level)
	% the field of StructData.
	% StructData.name, StructData.role,StructData.data and StructData.levelname 
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
		StructData.data={data};
		StructData.levelname=levelname;
		
	else
		disp("Error: type: Output arguments must be 3 or 4 for level values")
	end
end