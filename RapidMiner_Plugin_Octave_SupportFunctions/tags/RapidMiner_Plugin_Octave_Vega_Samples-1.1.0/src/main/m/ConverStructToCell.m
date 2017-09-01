function [OutputCell]=ConverStructToCell(InputStruct)
	%%%%%%%%%%%%%%%%%%%%%%%
	% Create the Cell data dynamic , it can find the field in Struct data 
	% if structSize == 4 , it is mean there is special attribute, not finish
	% N numbre of attributes
	% M length of field, for the data that is in an cell 1 x 1, so M=1 we get data form this cell 
	% When the data is made in Struct ;the field 'data' is not the last field in ExempleSet 
	% we change it ,'data' must be the 3e field.
	%%%%%%%%%%%%%%%%%%%%%%%
	structSize = length(fieldnames(InputStruct));
	field=fieldnames(InputStruct);
	numattribute=length(field(1));
	eval(['N=length(InputStruct','.',field{1},');']);
	indexrole=[];
	
	if structSize ~= 4 
		OutputCell=cell(structSize,N);
		for i=1:structSize
		
			eval(['M=length(InputStruct','.',field{i},');']);
		
			if 	M ~= 1
		
				for j=1:M
			
				eval(['OutputCell(',int2str(i),',',int2str(j),')=InputStruct','.',field{i},'(',int2str(j),');']);
		
				end

			else
	
				for j=1:N

				eval(['OutputCell(',int2str(i),',',int2str(j),')=InputStruct','.',field{i},'{1}(:,',int2str(j),');']);
		
				end
		
			end
		end
	
	else
		%find index in data for special attribute
		levelname=[];
		OutputCell=cell(structSize-1,N);
		D1=InputStruct.levelname;
		%rolename=D1{1};
		disp("There is special attribute,we create data with last colnum 0")
		%for i=1:length(InputStruct.levelname)
			
			%levelname = [levelname;D1{i}];
		%end
		
		
	
				
		for i=1:structSize-1
		
			eval(['M=length(InputStruct','.',field{i},');']);
		
			if 	M ~= 1
		
				for j=1:M
			
					eval(['OutputCell(',int2str(i),',',int2str(j),')=InputStruct','.',field{i},'(',int2str(j),');']);
		
				end

			else
				
				for k=1:N
					if ~isempty(OutputCell{2,k})
						indexrole=k;
					end
				end
				%bak
				%for k=1:N
				%	if regexp(OutputCell{2,k},rolename)
				%		indexrole=k;
				%	end
				%end
				
				for j=1:N
					
					if j == indexrole
					
					
						%bak ,cell exempleSet for special attribute is Nx1 for each element
						eval(['OutputCell{',int2str(i),',',int2str(j),'}=D1;']);
					elseif j < indexrole
					
						eval(['OutputCell(',int2str(i),',',int2str(j),')=InputStruct','.',field{i},'{1}(:,',int2str(j),');']);
						
					else
						eval(['OutputCell(',int2str(i),',',int2str(j),')=InputStruct','.',field{i},'{1}(:,',int2str(j-1),');']);
						
					end
					
				end
		
			end
			
			
		end
		
		
		
	
	end
	
	for i=1:structSize
		if regexp(field{i},'data')
			local=i;
		end
	end
	temp=OutputCell(local,:);
	%change data to 3er place
	OutputCell(local,:)=OutputCell(3,:);
	OutputCell(3,:)=temp;
	
end