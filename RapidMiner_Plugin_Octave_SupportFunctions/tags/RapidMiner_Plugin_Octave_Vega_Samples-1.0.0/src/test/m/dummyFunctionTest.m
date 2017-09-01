function [X,Y] = dummyFunctionTest(K, E, S)
%selectione aleatoirement K points dans E et S et renvoie ces points dans X et Y
N=size(E)(1);
I=randperm(N);
J=I(1:K);
X=E(J,:);
Y=S(J,:);

end

