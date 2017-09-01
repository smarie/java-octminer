function [name role data varargout]=GetDataFromStruct(inputExempleSet)
	%%%%%%%%%%%%%%%%%
	%TODO try to make the data dynamic,
	% How to make output dynamic ? when we have the values;
	% Now i make it like , name ,role and data
	% name : 1xN cell
	% role : 1xN cell
	% data : MxN matrix
	% roledata : 1xN cell , values of special attribute not finishe
	%%%%%%%%%%%%%%%%%
	
	structSize = length(fieldnames(inputExempleSet));
	field=fieldnames(inputExempleSet);
	numattribute=length(field(1));
	eval(['N=length(inputExempleSet','.',field{1},');']);
	DataCell=cell(structSize,N);
	
	if structSize ~= 4
		disp("Struct with normal attributes")
		for i=1:3
			
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
		disp("Struct with special attributes")
		eval(['varargout{',int2str(1),'}=inputExempleSet','.',field{4},';'])
		name=inputExempleSet.name;
		role=inputExempleSet.role;
		if numname ~= numdata
		data=inputExempleSet.data{1};
		else
		data=inputExempleSet.data{1}(:,1:end-1);
		%roledata=inputExempleSet.roledata;
		nargout;
	end
	
	
end