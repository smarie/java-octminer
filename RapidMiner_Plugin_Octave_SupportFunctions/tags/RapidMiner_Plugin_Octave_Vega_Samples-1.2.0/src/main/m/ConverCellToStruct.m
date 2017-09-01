function [OutputStruct]=ConverCellToStruct(InputCell)

	%%%%%%%%%%%%%%%%%%%%%%%
	% Create the Struct data ,
	% field : name 1xN is name of attributes
	% field : role 1xN is role of attributes
	% field : data M x N is date of attributes matrix
	% if there is an attributes not numeric ,we create field roledata 
	% field : roledata 1xN+1 
	% 		roledata = {name of special attribute}{values1,values2,.....}	
	% Now only for one special attribute (16/05)
	% C=cell(N,2), N numbre of  special attribute
	%%%%%%%%%%%%%%%%%%%%%%%
	
	CellSize = length(InputCell);
	datavalues=[];
	levelname =[];
	indexrole=0;
	if CellSize == 3
		
		N = size(InputCell (1,:),2);
			
		namevalues=InputCell(1,:);
		rolevalues=InputCell(2,:);
			
		for i=1:N
		
			if ischar(InputCell {3,i}) | ~isempty(InputCell {2,i})
				levelname=[levelname InputCell{3,i}(1)];
				Ctemps=InputCell{3,i};
				indexrole=i;
			elseif  iscell(InputCell{3,i})
				Temp=[];
				r=length(InputCell{3,i});
				for i=1:r
					Temp= [Temp ; InputCell{3,i}{i}];
				end
				datavalues=[datavalues Temp];
			else
				datavalues=[datavalues InputCell{3,i}];
			end
		end
		OutputStruct.name=namevalues;
		OutputStruct.role=rolevalues;
		OutputStruct.data={datavalues};
		
		if ~isempty(levelname)
			C=cell(1,1);
			%C(1)=InputCell(1,indexrole);
			%C(1)=levelname;
			%OutputStruct.levelname={levelname};
			disp("There is special attribute,we create data with last colnum 999")
			Data=OutputStruct.data{1};
			
			Data=[Data ones(size(Data,1),1)*999];
			OutputStruct.levelname=Ctemps;
			OutputStruct.data={Data};
		end
		
	else if CellSize > 3
	
		N = size(InputCell (:,1),1);
		if N < 3
			disp("Error: size fo Cell must > 3 ")
		else
			namevalues=InputCell(1,:);
			rolevalues=InputCell(2,:);
			
			for i=1:CellSize
				if  ~isempty(InputCell {2,i}) | ischar(InputCell {3,i}) 
					
					indexrole=i;
					levelname=[levelname InputCell{3,i}(1)]; %bak for exemplset cell
					Ctemps=InputCell{3,i};	
					%bak for exemplset cell
				else
					
					if iscell(InputCell{3,i})
						r=length(InputCell{3,i});
						Temp=[];
						
						for rol=1:r
							Temp= [Temp ; InputCell{3,i}{rol}];
						end
						datavalues=[datavalues Temp];
					else
						datavalues=[datavalues InputCell{3,i}];
					end
				end
			end
		end
		
		OutputStruct.name=namevalues;
		OutputStruct.role=rolevalues;
		OutputStruct.data={datavalues};
		
		if ~isempty(levelname)
			disp("111111111111111")
			%C=cell(1,1);
			%C(1)=InputCell(1,indexrole);
			%C(1)=levelname;
			[r c]=size(levelname);
			levelname1=[];
			%bak for exempleset
			%for j=1:c
				%for i=1:r
				%	levelname1{i,c} = levelname(i,c);
				%end
			%end
			
			%if there is levelname , we add an colmun with values 999 for Rapidminer ExemplSet;
			%because in Rapidminer , when an ExempleSet is created ,the size of data = numbre of attribute + numbre of speciale
			%
			disp("There is special attribute,we create data with last colnum 999")
			Data=OutputStruct.data{1};
			
			Data=[Data ones(size(Data,1),1)*999];
			OutputStruct.levelname=Ctemps;
			OutputStruct.data={Data};
		end
		
	else
		disp("Error: type: input arguments must be Cell and size fo Cell must > 3 ")
	end
	
end