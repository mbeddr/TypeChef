#if (definedEx(A))
int
#else
double
#endif
a;

#if (definedEx(B))
int
#else
double
#endif
b;

void main() {
	double i = 0;
	i = a + b;
}