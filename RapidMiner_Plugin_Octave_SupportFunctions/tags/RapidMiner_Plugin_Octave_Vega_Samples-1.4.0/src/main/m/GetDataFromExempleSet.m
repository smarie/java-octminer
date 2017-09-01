function [name role data varargout] =GetDataFromExempleSet(inputExampleSet)
	%%%%%%%%%%%%%%%%%
	%
	% How to make output dynamic ? when we have the values;
	% Now i make it like , name ,role and data
	% name : 1xN cell
	% role : 1xN cell
	% data : MxN matrix
	%%%%%%%%%%%%%%%%%
	
	if iscell(inputExampleSet) == 1 
	
		[name role data levelname subdata datacomplet]=GetDataFromCell(inputExampleSet);
		varargout{1}=levelname;
		varargout{2}=subdata;
		varargout{3}=datacomplet;
		%varargout{1}='there is no roledata for cell type';
	else if isstruct(inputExampleSet) == 1 
	
		[name role data levelname subdata datacomplet]=GetDataFromStruct(inputExampleSet);
		varargout{1}=levelname;
		varargout{2}=subdata;
		varargout{3}=datacomplet;
	else 
		disp("Error: type: input arguments must be Cell or Struct")
	end
	
end