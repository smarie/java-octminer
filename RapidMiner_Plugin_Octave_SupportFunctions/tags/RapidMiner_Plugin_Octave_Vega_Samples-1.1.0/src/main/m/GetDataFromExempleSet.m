function [name role data varargout] =GetDataFromExempleSet(inputExempleSet)
	%%%%%%%%%%%%%%%%%
	%
	% How to make output dynamic ? when we have the values;
	% Now i make it like , name ,role and data
	% name : 1xN cell
	% role : 1xN cell
	% data : MxN matrix
	%%%%%%%%%%%%%%%%%
	
	if iscell(inputExempleSet) == 1 
	
		[name role data roledata]=GetDataFromCell(inputExempleSet);
		varargout{1}=roledata;
		%varargout{1}='there is no roledata for cell type';
	else if isstruct(inputExempleSet) == 1 
	
		[name role data roledata]=GetDataFromStruct(inputExempleSet);
		varargout{1}=roledata;
		
	else 
		disp("Error: type: input arguments must be Cell or Struct")
	end
	
end