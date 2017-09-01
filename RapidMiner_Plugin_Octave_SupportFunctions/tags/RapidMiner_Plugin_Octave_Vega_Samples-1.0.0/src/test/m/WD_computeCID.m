function DistanceMatrix = WD_computeCID(data,parameters)
% function WD_computeCID
% Compute  complexity invariance distance of a data matrix
% Ref : A Complexity-Invariant Distance Measure for Time Series 
% Gustavo Batista, Xiaoyue Wang, Eamonn Keogh, 2011  
% 
% 2011/05 F. Suard, H. Najmeddine CEA/LIST/LIMA, projet wareHouses Data

verbose = 0;
if nargin > 2,
    if isfield(parameters,'verbose'),
        verbose = parameters.verbose;
    end
end


[nbData,nbDim] = size(data);

DistanceMatrix = zeros(nbData);

for i=1:nbData,
    A = data(i,:);
    CE_A=sqrt(sum(diff(A).^2));
    
    for j=i:nbData,
        B = data(j,:);
        CE_B=sqrt(sum(diff(B).^2));
       
        temp = norm(A-B)*(max(CE_A,CE_B)/(min(CE_A,CE_B)+eps)); % 1/length(x)*norm(x-y)*(max(CE_A,CE_B)/min(CE_A,CE_B));
        DistanceMatrix(i,j) = temp;
        DistanceMatrix(j,i) = temp;
    end
end

DistanceMatrix = DistanceMatrix./nbDim;

