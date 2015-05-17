package edu.asu.irs13;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Scanner;
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

public class Assign1 {
	
	public static void main(String[] args)  throws Exception
	{
		IndexReader r = IndexReader.open(FSDirectory.open(new File("index")));
		
		int doc_loop_control = 0, curFreqSum, resultHolder, totNoDocs = r.maxDoc();
		double curIDF=0;
		int currentCount;
		double queryL2Norm = 0.00, cosDist = 0.00;
		String queryTerms[] = new String[10]; //Assuming that the query has only 10 elements
		
		long prev,startTime;
		prev = startTime = System.currentTimeMillis();

		HashMap<String,Integer> queryTf = new HashMap<String, Integer>();
		HashMap<Integer, Double> queryResDis = new HashMap<Integer, Double>();
		HashMap<Integer, Double> querySorted = new HashMap<Integer, Double>();
		HashSet<Integer> qryHitDocs = new HashSet<Integer>();

		HashMap<Integer, HashMap<String, Integer>> hash_TD_outer = new HashMap<Integer, HashMap<String, Integer>>();
		HashMap<String, Integer> hash_TD_inner;
		HashMap<Integer, Double> hashTfVector = new HashMap<Integer, Double>();
		HashMap<Integer, Double> hashTfIdfVector = new HashMap<Integer, Double>();
		HashMap<String, Double> hashIDFVector = new HashMap<String, Double>();
		
		Scanner sc = new Scanner(System.in);
		File fout = new File("OutputFile.txt");
		FileOutputStream fos = new FileOutputStream(fout);
		BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));
		
//		-----------------------------------------------------------------------------------------------------------------------------------
//		Uncomment this line to print computation data to a file (Necessarycode has to beun-commented through the program as well.).
//		-----------------------------------------------------------------------------------------------------------------------------------
//		File fout1 = new File("ComputationalDataFile.txt");
//		FileOutputStream fos1 = new FileOutputStream(fout1);
//		BufferedWriter bw1 = new BufferedWriter(new OutputStreamWriter(fos1));
		
		System.out.println("Assign1 : The number of documents in this index is: " + totNoDocs);
		System.out.println("DEBUG ... TIME TAKEN IS :"+ (double)(System.currentTimeMillis() - prev)/1000 + " Seconds");
		prev = System.currentTimeMillis();
		
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
		System.out.println("TD matrix has been construted");
		System.out.println("DEBUG ... TIME TAKEN IS :"+ (double)(System.currentTimeMillis() - prev)/1000 + " Seconds");
		prev = System.currentTimeMillis();
		
//		-----------------------------------------------------------------------------------------------------------------------------------
//		Computing the TF L2Norm for each doc
//		-----------------------------------------------------------------------------------------------------------------------------------
		while (doc_loop_control <  totNoDocs)
		{
			if (hash_TD_outer.get(doc_loop_control) == null)
				System.out.println("The code did not work for doc number :"+doc_loop_control);

			hash_TD_inner = hash_TD_outer.get(doc_loop_control);
			curFreqSum = 0;

			for (Map.Entry<String, Integer> entry : hash_TD_inner.entrySet())
			{
				resultHolder = entry.getValue();
			    curFreqSum += (resultHolder * resultHolder);
			}//end of for

			hashTfVector.put(doc_loop_control, Math.sqrt(curFreqSum));
			doc_loop_control++;
		}//end of doc iteration loop
		System.out.println("TF vector has been computed");
		System.out.println("DEBUG ... TIME TAKEN IS :"+ (double)(System.currentTimeMillis() - prev)/1000 + " Seconds");
		prev = System.currentTimeMillis();
//		-----------------------------------------------------------------------------------------------------------------------------------
//		Uncomment this line to print TF L2Norms.
//		-----------------------------------------------------------------------------------------------------------------------------------
//		for (HashMap.Entry<Integer, Double> entry : hashTfVector.entrySet())
//		{
//			System.out.println("Document id = "+ entry.getKey()+" \t\t TF L2Norm = "+entry.getValue());
//		}
//		for (HashMap.Entry<Integer, Double> entry : hashTfVector.entrySet())
//		{
//			bw1.write("Document id = "+ entry.getKey()+" \t\t TF L2Norm = "+entry.getValue()+"\n");
//		}

//		-----------------------------------------------------------------------------------------------------------------------------------
//		Computing the IDF for each doc
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
		System.out.println("IDF vector has been computed");
		System.out.println("DEBUG ... TIME TAKEN IS :"+ (double)(System.currentTimeMillis() - prev)/1000 + " Seconds");
		prev = System.currentTimeMillis();
//		-----------------------------------------------------------------------------------------------------------------------------------
//		Uncomment this line to print IDF for each term
//		-----------------------------------------------------------------------------------------------------------------------------------
//		for (HashMap.Entry<String, Double> entry : hashIDFVector.entrySet())
//		{
//			System.out.println("Term = "+ entry.getKey()+" \t\tIDF = "+entry.getValue());
//		}
//		for (HashMap.Entry<String, Double> entry : hashIDFVector.entrySet())
//		{
//			bw1.write("Term = "+ entry.getKey()+" \t\tIDF = "+entry.getValue()+"\n");
//		}
		
//		-----------------------------------------------------------------------------------------------------------------------------------
//		Computing the TF/IDF L2Norm for each doc
//		-----------------------------------------------------------------------------------------------------------------------------------
		doc_loop_control = 0;
		while (doc_loop_control <  totNoDocs)
		{
			if (hash_TD_outer.get(doc_loop_control) == null)
				System.out.println("The code did not work for doc number :"+doc_loop_control);

			hash_TD_inner = hash_TD_outer.get(doc_loop_control);
			curIDF = 0;

			for (Map.Entry<String, Integer> entry : hash_TD_inner.entrySet())
			{
				double resultHolder1 = (double) entry.getValue();
				curIDF += (resultHolder1 * resultHolder1 * hashIDFVector.get(entry.getKey()) * hashIDFVector.get(entry.getKey()));
			}//end of for

			hashTfIdfVector.put(doc_loop_control, Math.sqrt(curIDF));
			doc_loop_control++;
		}//end of doc iteration loop
		System.out.println("TF/IDF vector has been computed");
		System.out.println("DEBUG ... TIME TAKEN IS :"+ (double)(System.currentTimeMillis() - prev)/1000 + " Seconds");
		prev = System.currentTimeMillis();
//		-----------------------------------------------------------------------------------------------------------------------------------
//		Uncomment this line to print TF/IDF L2Norms.
//		-----------------------------------------------------------------------------------------------------------------------------------
//		for (HashMap.Entry<Integer, Double> entry : hashTfIdfVector.entrySet())
//		{
//			System.out.println("Document id = "+ entry.getKey()+" \t\tTF/IDF L2Norm = "+entry.getValue());
//		}
//		for (HashMap.Entry<Integer, Double> entry : hashTfIdfVector.entrySet())
//		{
//			bw1.write("Document id = "+ entry.getKey()+" \t\tTF/IDF L2Norm = "+entry.getValue()+"\n");
//		}

//		-----------------------------------------------------------------------------------------------------------------------------------
//		Query and related operations
//		-----------------------------------------------------------------------------------------------------------------------------------
		System.out.print("query> ");
		String queryArray[] = {"grades", "newsletter", "carl hayden", "fall semester", "stimulant web" };
		for (String queryString : queryArray )
//		String queryString;
//		while(!(queryString = sc.nextLine()).equals("quit"))
		{
			queryTerms = queryString.split(" ");
			double queryStartTime = System.currentTimeMillis();
			
//			------------------------------------------------query TF, l2norm computation--------------------------------------------------
			for(String searchWord : queryTerms)
			{
				currentCount = queryTf.get(searchWord) == null ? 0 : queryTf.get(searchWord);
				queryTf.put(searchWord, currentCount+1);
			}//end of for each term 
			currentCount = 0;
			for (Map.Entry<String, Integer> entry : queryTf.entrySet())
			{
				currentCount += entry.getValue() * entry.getValue();
			}//end of for each element in queryTF
			queryL2Norm = Math.sqrt(currentCount);
			System.out.println(" The L2 norm of the Query is :"+queryL2Norm);

//		------------------------------------------------hit docs list generation----------------------------------------------------------			
			t = r.terms();
			for (Map.Entry<String, Integer> entry : queryTf.entrySet())
			{
				Term te = new Term("contents", entry.getKey());
				TermDocs td = r.termDocs(te);
				while (td.next())
				{
					qryHitDocs.add(td.doc());
				}//end of while for every document
			}//end of for each element in queryTF

//		---------------------------------------------TF cosine distance generation---------------------------------------------------------
			bw.write("\n\n\n\n\n\n************************************** QUERY IS '"+queryString+"' ************************************************\n\n");
			bw.write("\n\n************************************** TF cosine distance ************************************************\n\n");
			for (int docId : qryHitDocs)
			{
				hash_TD_inner = hash_TD_outer.get(docId);
				cosDist = 0.00;
				for (Map.Entry<String, Integer> entry : queryTf.entrySet())
				{
					if (hash_TD_inner.get(entry.getKey()) != null)
						cosDist += hash_TD_inner.get(entry.getKey()) * entry.getValue();
				}//end of for each term in the query
				cosDist = cosDist / (queryL2Norm * hashTfVector.get(docId));
				queryResDis.put(docId, cosDist);
			}// end of for each doc in hit docs
			querySorted = sortByComparator(queryResDis);
			System.out.println("TF : The number of results for '"+ queryString + "' are " + querySorted.size());
			for (Map.Entry<Integer, Double> entry : querySorted.entrySet())
			{
				bw.write(entry.getKey()+"\t\t\t\t"+entry.getValue()+"\n");
			}//end of for each term in the query
			queryResDis.clear();
			querySorted.clear();

			bw.write("\n\n************************************** TF/IDF cosine distance ********************************************\n\n");
			for (int docId : qryHitDocs)
			{
				hash_TD_inner = hash_TD_outer.get(docId);
				cosDist = 0.00;
				for (Map.Entry<String, Integer> entry : queryTf.entrySet())
				{
					if (hash_TD_inner.get(entry.getKey()) != null)
						cosDist += hash_TD_inner.get(entry.getKey()) * entry.getValue();
				}//end of for each term in the query
				cosDist = cosDist / (queryL2Norm * hashTfIdfVector.get(docId));
				queryResDis.put(docId, cosDist);
			}// end of for each doc in hit docs
			querySorted = sortByComparator(queryResDis);
			System.out.println("TF/IDF : The number of results for '"+ queryString + "' are " + querySorted.size());
			for (Map.Entry<Integer, Double> entry : querySorted.entrySet())
			{
				bw.write(entry.getKey()+"\t\t\t\t"+entry.getValue()+"\n");
			}//end of for each term in the query
			queryResDis.clear();
			querySorted.clear();

			queryTf.clear();
			qryHitDocs.clear();
			System.out.println("The query time is "+ (System.currentTimeMillis() - queryStartTime));
			System.out.print("\nquery> ");
		}//end of query while
		sc.close();
		bw.close();
//		-----------------------------------------------------------------------------------------------------------------------------------
//		Uncomment this if you are printing computational details to a file
//		-----------------------------------------------------------------------------------------------------------------------------------
//		bw1.close();
		System.out.println("DEBUG ... TIME TAKEN IS :"+ (double)(System.currentTimeMillis() - prev)/1000 + "seconds" + " and Total time taken is "+ (System.currentTimeMillis() - startTime)/1000 + " seconds");
	}// end of main

	public static HashMap<Integer, Double> sortByComparator(HashMap<Integer, Double> unsortMap)
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
    }

}// end of class