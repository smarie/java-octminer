function CellData= testm(data,varargin)
	%%%%%%%%%%%%%%%%%
	%
	% varargin is input var dynamic , it have 3 type possibly
	% 1, name
	% 2, name ,role 
	% 3, name ,role ,levelname (if the role are not empty)
	% name : 1xN cell
	% role : 1xN cell
	% levelname : M x num (num is numebre of role not empty)
	% data : MxN matrix complet 
	%%%%%%%%%%%%%%%%%
	[nbData nbDim] = size(data);
	
	n=length(varargin);
	
	if n==0 %no name, on role input 
		disp("no name, role input ")
		CellData=cell(3,nbDim);
		for i=1:nbDim
			
			attName= [ 'att' num2str(i)];
			CellData{1,i}=attName;
			CellData{2,i}="";
			CellData{3,i}=data(:,i);
		end
	elseif n==1 % only name input 
		disp("only name input ")
		name=varargin{1};
		n=length(name);
		CellData=cell(3,n);
		
		CellData(1,:)=name;
		for i=1:nbDim
			CellData{2,i}=""; 
			CellData{3,i}=data(:,i);
		end
	elseif n==2 % name and role input 
		disp("name and role input ")
		name=varargin{1};
		role=varargin{2};
		n=length(name);
		CellData=cell(3,n);
		CellData(1,:)=name;
		CellData(2,:)=role;
		for i=1:nbDim
			
			CellData{3,i}=data(:,i);
		
		end
		
		
	else
		disp("name , role and levelname input ")
		name=varargin{1};
		role=varargin{2};
		levelname=varargin{3};
		n=length(name);
		CellData=cell(3,n);
		CellData(1,:)=name;
		CellData(2,:)=role;
		Celltemps=[];
		for i=1:nbDim
			
		if  cellfun ( @isempty, (levelname(i)))== 0
			CellData{3,i}=levelname {i}(data(:,i));	
		else
			CellData{3,i}=data(:,i);		
			
		end
		end
		
		
	end
end