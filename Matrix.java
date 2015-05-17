package edu.asu.irs13;

public class Matrix {
	double [][] matrix;
	int noCols, noRows;
	
	public Matrix() // default constructor .. does nothing :P
	{
		matrix = null;
		noCols = 0;
		noRows = 0;
	}
	
	public Matrix(int rows, int cols)// init matrix with dimentions cols*rows
	{
		matrix = new double[rows][cols];
		noCols = cols;
		noRows = rows;
	}
	
	public Matrix(Matrix m) // copy construtor
	{
		matrix = new double[m.noRows][m.noCols];
		noCols = m.noCols;
		noRows = m.noRows;
		int i,j;
		for(i=0; i<m.noRows; i++)
		{
			for(j=0; j<m.noCols; j++)
				matrix[i][j] = m.matrix[i][j];
		}
	}
	
	void set(int x, int y, double val)// sets the value of a element
	{
		matrix[x][y] = val;
	}
	
	double get(int x,  int y)// gets the value of an element
	{
		return matrix[x][y];
	}
	
	public void transpose()// performs tranpose of the matrix
	{
		Matrix temp = new Matrix(this);
		int i,j;
		this.noCols = temp.noRows;
		this.noRows = temp.noCols;
		for(i=0; i<noRows; i++)
		{
			for(j=0; j<noCols; j++)
				matrix[i][j] = temp.matrix[j][i];
		}
	}
	
	public void print() // prints the matrix
	{
		int i,j;
		for(i=0; i<noRows; i++)
		{
			for(j=0; j<noCols; j++)
				System.out.print(matrix[i][j] + " ");
			System.out.println();
		}
	}
	
    public Matrix add(Matrix B) // return C = A + B
    {
        Matrix A = this;
        if (B.noRows != A.noRows || B.noCols != A.noCols) throw new RuntimeException("Illegal matrix dimensions.");
        Matrix C = new Matrix(noRows, noCols);
        for (int i = 0; i < noRows; i++)
            for (int j = 0; j < noCols; j++)
                C.matrix[i][j] = A.matrix[i][j] + B.matrix[i][j];
        return C;
    }

    public Matrix subract(Matrix B) // return C = A - B
    {
        Matrix A = this;
        if (B.noRows != A.noRows || B.noCols != A.noCols) throw new RuntimeException("Illegal matrix dimensions.");
        Matrix C = new Matrix(noRows, noCols);
        for (int i = 0; i < noRows; i++)
            for (int j = 0; j < noCols; j++)
                C.matrix[i][j] = Math.abs(A.matrix[i][j] - B.matrix[i][j]);
        return C;
    }

    public boolean equals(Matrix B) // does A = B exactly?
    {
        Matrix A = this;
        if (B.noRows != A.noRows || B.noCols != A.noCols) throw new RuntimeException("Illegal matrix dimensions.");
        for (int i = 0; i < noRows; i++)
            for (int j = 0; j < noCols; j++)
                if (A.matrix[i][j] != B.matrix[i][j]) return false;
        return true;
    }

    public Matrix multiply(Matrix B) // return C = A * B
    {
        Matrix A = this;
        if (A.noCols != B.noRows) throw new RuntimeException("Illegal matrix dimensions. found " + A.noCols + " and " + B.noRows );
        Matrix C = new Matrix(A.noRows, B.noCols);
        for (int i = 0; i < C.noRows; i++)
            for (int j = 0; j < C.noCols; j++)
                for (int k = 0; k < A.noCols; k++)
                    C.matrix[i][j] += (A.matrix[i][k] * B.matrix[k][j]);
        return C;
    }

    public void init(double val) // initialize a matrix with a integer 
    {
    	int i,j;
    	for(i=0; i<noRows; i++)
    	{
    		for(j=0; j<noCols; j++)
    			matrix[i][j] = val;
    	}
	}

    public void assign(Matrix m)
    {
    	int i,j;
    	for(i=0; i<noRows; i++)
    	{
    		for(j=0; j<noCols; j++)
    			matrix[i][j] = m.matrix[i][j];
    	}
    }

    public void normalize() 
    {
		double colSqSum = 0.0;
		int i,j;
		for (i=0; i<noCols; i++)
		{
			colSqSum = 0.0;
			for (j=0;j<noRows; j++)
				colSqSum += matrix[j][i]*matrix[j][i];
			for (j=0;j<noRows; j++)
				matrix[j][i] = matrix[j][i]/ Math.sqrt(colSqSum);
		}
	}

    public void l1Norm()
    {
    	double sum = 0.0;
    	for (int i=0; i<noRows; i++)
    		sum += matrix[i][0];
    	for(int i=0; i<noRows; i++)
    		matrix[i][0] = matrix[i][0] / sum;
    }
    
    public double getMax() 
    {
		double result = -9999.0;
		int i,j;
		for(i=0;i<noCols;i++)
		{
			for (j=0; j<noRows; j++)
				if (matrix[j][i]>result)
					result = matrix[j][i];
		}
		return result;
	}
    
    public double getMin()
    {
    	double result = 9999.0;
		int i,j;
		for(i=0;i<noCols;i++)
		{
			for (j=0; j<noRows; j++)
				if (matrix[j][i]<result)
					result = matrix[j][i];
		}
		return result;
    }

}
