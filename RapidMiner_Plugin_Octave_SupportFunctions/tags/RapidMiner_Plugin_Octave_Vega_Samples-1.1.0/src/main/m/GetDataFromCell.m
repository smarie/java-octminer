function [name role data varargout]=GetDataFromCell(inputExempleSet);
	%%%%%%%%%%%%%%%%%
	% TODO try to make the data dynamic,
	% How to make output dynamic ? when we have the values;
	% Now i make it like , name ,role and data
	% name : 1xN cell
	% role : 1xN cell
	% data : MxN cell
	% if numbre of attributes > 3 , CellSize=numbre of attributes
	%%%%%%%%%%%%%%%%%
	CellSize = length(inputExempleSet);
	data =[];
	
	if CellSize == 3
		
		N = size(inputExempleSet (1,:),2);
			
			name=inputExempleSet(1,:);
			role=inputExempleSet(2,:);
			
			for i=1:N
			
				if ~ischar(inputExempleSet {3,i})
					
					nargout
					data=[data inputExempleSet{3,i}];
					varargout{i}='there is no roledata for this Struct Data';
				else
				
					
					%C=cell(1,2);
					%C(1)=inputExempleSet(1,i);
					C=inputExempleSet{3,i};
					%eval(['varargout{',int2str(1),'}=inputExempleSet {3,',int2str(i),'};'])
					eval(['varargout{',int2str(1),'}=C;'])
				end
			end
			
	else if CellSize > 3
	
			N = size(inputExempleSet (:,1),1);
			if N < 3
				disp("Error: size fo Cell must > 3 ")
			else
				name=inputExempleSet(1,:);
				role=inputExempleSet(2,:);
				data=[];
				for i=1:CellSize
				
					if ~iscell(inputExempleSet {3,i}) 
						data=[data inputExempleSet{3,i}];
						varargout{i}='there is no roledata for this Struct Data';
					
					else
					%C=cell(1,2);
					%C(1)=inputExempleSet(1,i);
					r=length(inputExempleSet{3,i});
					levelname1=[];
					
						for j=1:r
							levelname1{j,1} = inputExempleSet{3,i}{j};;
						end
					
					C=levelname1;
					%eval(['varargout{',int2str(1),'}=inputExempleSet {3,',int2str(i),'};'])
					eval(['varargout{',int2str(1),'}=C;'])
					%eval(['varargout{',int2str(1),'}=inputExempleSet {3,',int2str(i),'};'])
					end
				end
			end
	else
		disp("Error: type: input arguments must be Cell and size fo Cell must > 3 ")
	end
	
end
