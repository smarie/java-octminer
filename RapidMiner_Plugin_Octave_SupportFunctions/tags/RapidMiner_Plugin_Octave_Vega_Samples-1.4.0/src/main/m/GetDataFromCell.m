function [name role data varargout]=GetDataFromCell(inputExempleSet);
	%%%%%%%%%%%%%%%%%
	% 
	% name : 1xN cell
	% role : 1xN cell
	% if role is not empty
	% data : M x N - ( numbre of special attribute ) for the data numeric
	% subdata : M x ( numbre of special attribute ) for the data nomial
	% else 	data : M x N Matrix
	% levelname : 1xN cell for save the special attribute ,if them exist;
	%varargout{1}=levelname;
	%varargout{2}= subdata, index of role ;
	%varargout{2}=[data subdata] matrix complet;
	%%%%%%%%%%%%%%%%%
	
	CellSize = length(inputExempleSet);
	data =[];
	subdata =[];
	levelname = cell();
	name=inputExempleSet(1,:);
	role=inputExempleSet(2,:);
	% find the element without the role ,the attribute values is put into matirx
	emptyCell=find(cellfun ( @isempty, (inputExempleSet))== 1);
	emptyData=inputExempleSet([emptyCell'+1]);
	data=cell2mat (emptyData);
			
	% % find the element with the role 
	tempfull=find(cellfun ( @isempty, (inputExempleSet))== 0);
	[varn indexn]=intersect(tempfull, emptyCell-1);
	tempfull (indexn)=[];
	[varr indexr]=intersect(tempfull, emptyCell+1);
	tempfull (indexr)=[];
	% in the arrays fullcell, the frist data element is 3rd ,aftre each 3 element is data, we copy their index in data;
	dataindex=inputExempleSet(tempfull(3:3:end));
	for i=1:length(dataindex)
		if ischar(dataindex{i,1}{1})
			[C indexIn indexOut]=unique (dataindex{i,1} );
			levelname{i+size(emptyData,2)}=C;
			subdata=[subdata indexOut-1];
		else
			[C indexIn indexOut]=unique(cell2mat(dataindex{i,1}));
			levelname{i+size(emptyData,2)}= num2cell(C);
			subdata=[subdata indexOut-1];
		end
	end
			varargout{1}=levelname;
			varargout{2}=subdata;
			varargout{3}=[data subdata];
		
	
	
end
