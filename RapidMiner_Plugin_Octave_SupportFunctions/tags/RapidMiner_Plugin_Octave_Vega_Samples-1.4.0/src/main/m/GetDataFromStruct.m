function [name role data varargout]=GetDataFromStruct(inputExempleSet)
	%%%%%%%%%%%%%%%%%
	%TODO try to make the data dynamic,
	% How to make output dynamic ? when we have the values;
	% Now i make it like , name ,role and data
	% name : 1xN cell
	% role : 1xN cell
	% data is matrix numeric ,
	% if there is the field levelname ,
	% 		data : M x N - ( numbre of special attribute ) for the data numeric
	%		subdata : M x ( numbre of special attribute ) for the data nomial
	% else 	data : M x N
	% levelname : 1xN cell for save the special attribute ,if them exist;
	% 
	%	varargout{1}=inputExempleSet.levelname;
	%	varargout{2}=subdata ,index of role ;;
	%	varargout{3}=datatemp,matrix complet;;
	%%%%%%%%%%%%%%%%%
	
	structSize = length(fieldnames(inputExempleSet));
	field=fieldnames(inputExempleSet);
	numattribute=length(field(1));
	eval(['N=length(inputExempleSet','.',field{1},');']);
	DataCell=cell(structSize,N);
	
	if structSize ~= 4
		%disp("Struct with normal attributes")
		for i=1:structSize
			
			eval(['varargout{',int2str(i),'}=inputExempleSet','.',field{i},';'])
			name=inputExempleSet.name;
			role=inputExempleSet.role;
			data=inputExempleSet.data{1};
			
			nargout;
			varargout{1}='there is no roledata for this Struct Data';

		end
	
	else
		numname=size(inputExempleSet.name);
		numdata=size(inputExempleSet.data{1});
		%disp("Struct with special attributes")
		%eval(['varargout{',int2str(1),'}=inputExempleSet','.',field{4},';'])
		name=inputExempleSet.name;
		role=inputExempleSet.role;
		%index=find(cellfun ( @isempty, (role))== 0);
		datatemp=inputExempleSet.data{1};
		subdata = datatemp(:,find(cellfun ( @isempty, (role))== 0));
		data=datatemp(:,find(cellfun ( @isempty, (role))== 1));
		varargout{1}=inputExempleSet.levelname;
		varargout{2}=subdata;
		varargout{3}=datatemp;
		
	end
	
	
end