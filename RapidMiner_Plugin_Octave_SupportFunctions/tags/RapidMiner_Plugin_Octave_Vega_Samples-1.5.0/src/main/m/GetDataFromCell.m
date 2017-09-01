function [name role data varargout]=test(inputExempleSet);
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
	%data=cell2mat (emptyData);
	%find the data numeric

	for i = 1 : size(inputExempleSet,2)
	
		if cellfun ( @iscell, inputExempleSet(3,i)) == 0
			data = [data inputExempleSet{3,i}];
			levelname{i}=[];
		else 
			[C indexIn indexOut]=unique (inputExempleSet{3,i});
			levelname{i}=C;
			subdata=[subdata indexOut];
			data = [data indexOut];
			
		end
	end
			varargout{1}=levelname;
			varargout{2}=subdata;
	
end
