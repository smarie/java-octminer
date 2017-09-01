function coord = processIsomap(DistanceMatrix)
%%%% projection
options.dims = 2;
options.display = 0;
options.verbose = 0;
nbData = size(DistanceMatrix,1);
nbNeighbours = min([round(nbData./2),30]);

[Y, R, E] = Isomap(DistanceMatrix, 'k', nbNeighbours, options);
coord = Y.coords{1}';