/*
 * Code for Assignment 2 of CSE 598 : Information ret
 * author : Vignesh Jayabalan
 * Running instructions 
 * 	* To run the standard list of queries uncomment lines 484 - 490 and comment lines 493 - 512
 *  * To input query in runtime, comment lines 484 - 490 and uncomment lines 493 - 512
 */

package edu.asu.irs13;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermDocs;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.store.FSDirectory;

public class Assign2 {
	
	static HashMap<Integer, HashMap<String, Integer>> hash_TD_outer;
	static HashMap<String, Integer> hash_TD_inner;
	static HashMap<Integer, Double> hashTfVector, hashTfIdfVector;
	static HashMap<String, Double> hashIDFVector;
	static HashMap<Integer, Double> hubVector, authVector;
	static ArrayList<Integer> deadNodesList;
	static List<Integer> rootSet, baseSet;
	static LinkAnalysis analyser;
	static Matrix rank, authority, hub;
	static int totNoDocs;
	
	private static void init() throws Exception
	{
		hash_TD_outer = new HashMap<Integer, HashMap<String, Integer>>();
		hashTfVector = new HashMap<Integer, Double>();
		hashTfIdfVector = new HashMap<Integer, Double>();
		hashIDFVector = new HashMap<String, Double>();
		hubVector = new HashMap<Integer, Double>();
		authVector = new HashMap<Integer, Double>();
		totNoDocs = 25054;
		LinkAnalysis.numDocs = totNoDocs;
		rank = new Matrix(totNoDocs,1);
		analyser = new LinkAnalysis();
		deadNodesList = new ArrayList<>();
//		detecting all the dead nodes to be used in rank computation
		for (int i=0; i<totNoDocs; i++)
		{
			if (analyser.getLinks(i).length == 0)
				deadNodesList.add(i);
		}
	}
	
	private static void preCompute() throws Exception //Computer TD,TF, IDF, TF/IDF
	{
		int doc_loop_control = 0, resultHolder;
		double curIDF=0.0, curFreqSum;
		IndexReader r = IndexReader.open(FSDirectory.open(new File("index")));
		int totNoDocs = r.maxDoc();
		
//		-----------------------------------------------------------------------------------------------------------------------------------		
//		Computing the TD matrix
//		-----------------------------------------------------------------------------------------------------------------------------------
		TermEnum t = r.terms(); // List of all terms
		while(t.next()) // for each term
		{
			Term te = new Term("contents", t.term().text());
			TermDocs td = r.termDocs(te); // all documents that match the selected word
			while (td.next())
			{
				hash_TD_inner = hash_TD_outer.get(td.doc()) == null? new HashMap<String, Integer>() : hash_TD_outer.get(td.doc());
				{
					hash_TD_inner.put(t.term().text(), td.freq());
				}
				hash_TD_outer.put(td.doc(), hash_TD_inner);
			}//end of document while
		}//end of term while
		t.close();

//		-----------------------------------------------------------------------------------------------------------------------------------
//		Computing the TF L2Norm for each doc
//		-----------------------------------------------------------------------------------------------------------------------------------
		while (doc_loop_control <  totNoDocs)
		{
			if (hash_TD_outer.get(doc_loop_control) == null)
				System.out.println("The code did not work for doc number :"+doc_loop_control);

			hash_TD_inner = hash_TD_outer.get(doc_loop_control);
			curFreqSum = 0.0;

			for (Map.Entry<String, Integer> entry : hash_TD_inner.entrySet())
			{
				resultHolder = entry.getValue();
			    curFreqSum += ((double)resultHolder * (double)resultHolder);
			}//end of for

			hashTfVector.put(doc_loop_control, Math.sqrt(curFreqSum));
			doc_loop_control++;
		}//end of doc iteration loop
	
//		-----------------------------------------------------------------------------------------------------------------------------------
//		Computing the IDF for each terms
//		-----------------------------------------------------------------------------------------------------------------------------------
		t = r.terms(); // List of all terms
		while(t.next()) // for each term
		{
			Term te = new Term("contents", t.term().text());
			TermDocs td = r.termDocs(te); // list of docs that have the list
			int docCount = 0;
			while(td.next())
			{
				docCount++;
			}
			
			if (docCount!=0)
			{	
				double a = (double) totNoDocs;
				double b = (double) docCount;
				curIDF = Math.log(a/b);
				hashIDFVector.put(t.term().text(), curIDF);
			}	

		}// End of term while
		t.close();
//		-----------------------------------------------------------------------------------------------------------------------------------
//		Computing the TF/IDF L2Norm for each doc
//		-----------------------------------------------------------------------------------------------------------------------------------
		doc_loop_control = 0;
		while (doc_loop_control <  totNoDocs)
		{
			if (hash_TD_outer.get(doc_loop_control) == null)
				System.out.println("The code did not work for doc number :"+doc_loop_control);

			hash_TD_inner = hash_TD_outer.get(doc_loop_control);
			curIDF = 0.0;

			for (Map.Entry<String, Integer> entry : hash_TD_inner.entrySet())
			{
				double resultHolder1 = (double) entry.getValue();
				curIDF += (resultHolder1 * resultHolder1 * hashIDFVector.get(entry.getKey()) * hashIDFVector.get(entry.getKey()));
			}//end of for

			hashTfIdfVector.put(doc_loop_control, Math.sqrt(curIDF));
			doc_loop_control++;
		}//end of doc iteration loop
		
		t.close();

	}//end of pre-compute function
	
	//Accepts a query and returns a hash of DocId and TF/IDF similarity 
	private static HashMap<Integer, Double> fetchIdfSimilarity(String queryString) throws Exception
	{
		String queryTerms[] = new String[10]; //Assuming that the query has a max of 10 elements
		IndexReader r = IndexReader.open(FSDirectory.open(new File("index")));
		int currentCount=0;
		double queryL2Norm = 0.0, cosDist = 0.0;
		
		HashMap<String,Integer> queryTf = new HashMap<String, Integer>();
		HashMap<Integer, Double> queryResDis = new HashMap<Integer, Double>();
		HashMap<Integer, Double> querySorted = new HashMap<Integer, Double>();
		HashSet<Integer> qryHitDocs = new HashSet<Integer>();
		
		TermEnum t = r.terms(); // List of all terms

		queryTerms = queryString.split(" ");
		
//		------------------------------------------------query TF, l2norm computation--------------------------------------------------
		for(String searchWord : queryTerms)
		{
			currentCount = queryTf.get(searchWord) == null ? 0 : queryTf.get(searchWord);
			queryTf.put(searchWord, currentCount+1);
		}//end of for each term 
		currentCount = 0;
		for (Map.Entry<String, Integer> entry : queryTf.entrySet())
		{
			currentCount += (entry.getValue() * entry.getValue());
		}//end of for each element in queryTF
		queryL2Norm = Math.sqrt((double)currentCount);
		
//		---------------------------------------------TF/IDF cosine distance generation---------------------------------------------------------
		for (Map.Entry<String, Integer> entry : queryTf.entrySet())
		{
			qryHitDocs.clear();
			Term te = new Term("contents", entry.getKey());
			TermDocs td = r.termDocs(te);
			while (td.next())
			{
				qryHitDocs.add(td.doc());
			}//end of while for every document
			for (int docId : qryHitDocs)
			{
				hash_TD_inner = hash_TD_outer.get(docId);
				cosDist = queryResDis.get(docId) == null ? 0 : queryResDis.get(docId);
				cosDist += (double)hash_TD_inner.get(entry.getKey()) * hashIDFVector.get(entry.getKey());
				queryResDis.put(docId, cosDist);
			}
		}
		
//		--------------------------------------------Normalizing the query --------------------------------------------
		for (Map.Entry<Integer, Double> entry : queryResDis.entrySet())
		{
			entry.setValue(entry.getValue()/ (queryL2Norm * hashTfIdfVector.get(entry.getKey())));
		}
		
		querySorted = sortByComparator(queryResDis); //Sorting the results 
		t.close();
		return querySorted;
	}//end of fetchIdfRanks

	private static HashMap<Integer, Double> sortByComparator(HashMap<Integer, Double> unsortMap)
    {
        List<Entry<Integer, Double>> list = new LinkedList<Entry<Integer, Double>>(unsortMap.entrySet());
        // Sorting the list based on values
        Collections.sort(list, new Comparator<Entry<Integer, Double>>()
        {
            public int compare(Entry<Integer, Double> o1, Entry<Integer, Double> o2)
            {
                return o2.getValue().compareTo(o1.getValue());
            }
        });

        // Maintaining insertion order with the help of LinkedList
        HashMap<Integer, Double> sortedMap = new LinkedHashMap<Integer, Double>();
        for (Entry<Integer, Double> entry : list)
        {
            sortedMap.put(entry.getKey(), entry.getValue());
        }
        return sortedMap;
    } // End of sortByComparator

	//Accepts the base Set and return the root set.
	private static List<Integer> computeBaseSet(List<Integer> rootSet) 
	{
		ArrayList<Integer> result = new ArrayList<>();
		int[] tempCollector;
		
		for (int entry : rootSet)
		{
			if (!result.contains(entry))
				result.add(entry);
			tempCollector = analyser.getCitations(entry);
			for (int tempEntry : tempCollector)
			{
				if (!result.contains(tempEntry))
					result.add(tempEntry);
			}
			tempCollector = null;
			tempCollector = analyser.getLinks(entry);
			for (int tempEntry : tempCollector)
			{
				if (!result.contains(tempEntry))
					result.add(tempEntry);
			}
		}
		return result;
	}

	//Accepts a query and the value 'K' number of elements in the rootset.
	private static List<Integer> computeRootSet(String query, int number) throws Exception
	{
		HashMap<Integer, Double> querySorted = fetchIdfSimilarity(query);
		List<Integer> results = new ArrayList<>();
		for (Map.Entry<Integer, Double> entry : querySorted.entrySet())
		{
			results.add(entry.getKey());
			number--;
			if (number == 0)
				break;
		}
		return results;
	}
	
	//Performs adjacency calculation over a given list of nodes; used in compAuthHub 
	private static Matrix getAdjMatrix(List<Integer> baseSet) 
	{
		int size = baseSet.size();
		Matrix adj = new Matrix(size, size);
		int i,j;
		for(i=0; i<size; i++)
		{
			for(j=0; j<size; j++)
				adj.set(i, j, getAdjVal(i,j));
		}
		return adj;
	}
	
	//return 1 if there is a link between the two nodes.
	private static int getAdjVal(int i, int j) 
	{
		int[] tempLinks;
		int linkVar = 0;
		tempLinks = analyser.getLinks(i);
		for(int entry : tempLinks)
		{
			if (entry == j)
			{
				linkVar = 1;
				break;
			}
		}
		return linkVar;
	}

	private static double findError(Matrix authP, Matrix auth, Matrix hubP, Matrix hub) 
	{
		Matrix authDiff = authP.subract(auth);
		Matrix hubDiff = hubP.subract(hub);
		if (authDiff.getMax() >= hubDiff.getMax())
			return authDiff.getMax();
		else
			return hubDiff.getMax();
	}
	
	private static double findError(Matrix matP, Matrix matN) 
	{
		Matrix diff = matN.subract(matP);
		return diff.getMax();
	}
	
	//Computes the authority and hub values
	private static void compAuthHub(Matrix auth, Matrix hub, Matrix adj) 
	{
		Matrix adjT = new Matrix(adj);
		Matrix authP = new Matrix(auth);
		Matrix hubP = new Matrix(hub);
		adjT.transpose();
		
		double largestError = 1.0, threshold = Math.pow(10.0,-9.0);
		while (largestError > threshold) //the power iteration method
		{
			authP.assign(auth);
			hubP.assign(hub);
			auth.assign(adjT.multiply(hubP));
			hub.assign(adj.multiply(auth));
			auth.normalize();
			hub.normalize();
			largestError = findError(auth, authP, hub, hubP);
		}

		int i;
		for(i=0;i<baseSet.size();i++)
			authVector.put(baseSet.get(baseSet.size()-1-i), authority.get(i, 0));
		for(i=0;i<baseSet.size();i++)
			hubVector.put(baseSet.get(baseSet.size()-1-i), hub.get(i, 0));
		authVector = sortByComparator(authVector);
		hubVector = sortByComparator(hubVector);
		int cnt =0;
		for (Map.Entry<Integer, Double> entry : authVector.entrySet())
		{
			System.out.println(entry.getKey() + " has a auth of " + entry.getValue());
			cnt++;
			if (cnt == 10)
				break;
		}
		cnt =0;
		for (Map.Entry<Integer, Double> entry : hubVector.entrySet())
		{
			System.out.println(entry.getKey() + " has a hub of " + entry.getValue());
			cnt++;
			if (cnt == 10)
				break;
		}
	}

	//Computes one row for page rank computation
	private static Matrix computeRow(int row) 
	{
		Matrix m = new Matrix(1, totNoDocs);
		int linkCount;
		double c = 0.8, kVal = 1.0/totNoDocs, val=0.0;
		m.init((1.0-c)*kVal);
		for(int i : deadNodesList)
		{
			m.set(0, i, kVal);
		}
		int a[] = analyser.getCitations(row);
		for(int i: a)
		{
			linkCount = analyser.getLinks(i).length;
			val = (c/linkCount) + ((1.0-c) * kVal);
			m.set(0, i, val);
		}
		return m;
	}

	//Computes the page rank using power iteration
	private static void pageRank() throws Exception
	{
		Matrix prevRank = new Matrix(totNoDocs,1);
		Matrix m, result;
		int i;
		double error = 1.0, threshold =  Math.pow(10.0,-9.0), min, max;
		rank.init(1.0/totNoDocs);
		do 
		{
			prevRank.assign(rank);
			for (i=0; i<totNoDocs; i++)
			{
				m = computeRow(i);
				result = m.multiply(prevRank);
				rank.set(i,0,result.get(0,0));
			}
			rank.l1Norm();
			error = findError(prevRank, rank);
		}
		while(error>threshold);
		
		min = rank.getMin();
		max = rank.getMax();
		for (i=0; i<totNoDocs; i++)
		{
			rank.set(i, 0, ((rank.get(i, 0)-min)/(max-min)));
		}
	}
	
	//Auth and Hub starts here
	private static void authAndHub(String query, int noOfResults) throws Exception
	{
		authVector.clear();
		hubVector.clear();
		authority = null;
		hub = null;
		Matrix adj;
		rootSet = computeRootSet(query, noOfResults);
		Collections.sort(rootSet);
		baseSet = computeBaseSet(rootSet);
		Collections.sort(baseSet);
		adj = getAdjMatrix(baseSet);
		authority = new Matrix(baseSet.size(),1);
		authority.init(1.0);
		hub = new Matrix(baseSet.size(),1);
		hub.init(1.0);
		
		compAuthHub(authority,hub,adj);
		
		baseSet.clear();
		rootSet.clear();
	}

	//Combines the page rank with the TF/IDF similarity 
	private static void rankWithSim(String query, double w) throws Exception
	{
		double val=0.0;
		HashMap<Integer, Double> similarity = fetchIdfSimilarity(query);
		for (Map.Entry<Integer, Double> entry : similarity.entrySet())
		{
			val = (w * rank.get(entry.getKey(),0)) + ((1-w) * entry.getValue());
			similarity.put(entry.getKey(), val);
		}
		similarity = sortByComparator(similarity);
		val =0;
		for (Map.Entry<Integer, Double> entry : similarity.entrySet())
		{
			val++;
			System.out.println("The document Id is :" + entry.getKey() + " with combine rank and sim = " + entry.getValue());
			if (val == 10)
				break;
		}
	}
	
	public static void main(String args[]) throws Exception
	{
		int noOfResults = 10;
		double w = 0.4;
		String stars = "**************************************************************************";
		
		System.out.println("Please wait while I prepare myself for the queries :) ");
		
		init(); //initialize all the data objects 
		preCompute(); // perform the TF/IDF computations
		pageRank();// perform all the page ranks

		
		String[] queries={"campus tour", "transcripts", "admissions", "employee benefits", "parking decal", "src"};
		for (String query: queries)
		{
			System.out.println(stars + query + stars);
			authAndHub(query, noOfResults);//Computing authority and hubs!!
			rankWithSim(query, w);//Computing the page rank with the similarities
		}

		
/*		java.util.Scanner sc = new java.util.Scanner(System.in);
		System.out.print("query> ");
		String query = sc.nextLine();
		query = query.toLowerCase();
		while(!query.equals("quit"))
		{
			System.out.print("Enter the value of w :");
			w = sc.nextDouble();
			if (w>1)
			{
				System.out.println("invalid w");
				continue;
			}
			System.out.println(stars + query + stars);
			authAndHub(query, noOfResults);//Computing authority and hubs!!
			rankWithSim(query, w);//Computing the page rank with the similarities
			System.out.print("\n\n\n\nquery> ");
			sc.nextLine();
			query = sc.nextLine();
			query = query.toLowerCase();
		}
		sc.close();
*/		
		System.out.println("Gracefully exited !!");
	}
}

