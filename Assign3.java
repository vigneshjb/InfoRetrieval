package edu.asu.irs13;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermDocs;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.store.FSDirectory;

public class Assign3 
{	
	static HashMap<Integer, HashMap<String, Integer>> hash_TD_outer;
	static HashMap<String, Integer> hash_TD_inner;
	static HashMap<Integer, Double> hashTfVector, hashTfIdfVector;
	static HashMap<String, Double> hashIDFVector;
	static List<Integer> clusterVector ;
	static int totNoDocs;
	
	public static void init() throws Exception
	{
		hash_TD_outer = new HashMap<Integer, HashMap<String, Integer>>();
		hashTfVector = new HashMap<Integer, Double>();
		hashTfIdfVector = new HashMap<Integer, Double>();
		hashIDFVector = new HashMap<String, Double>();
		clusterVector = new ArrayList<>();
		totNoDocs = 25054;
	}
	
	public static void preCompute() throws Exception //Computer TD,TF, IDF, TF/IDF
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
	
	private static HashMap<String, Integer> sortByComp(HashMap<String, Integer> unsortMap)
    {
        List<Entry<String, Integer>> list = new LinkedList<Entry<String, Integer>>(unsortMap.entrySet());
        // Sorting the list based on values
        Collections.sort(list, new Comparator<Entry<String, Integer>>()
        {
            public int compare(Entry<String, Integer> o1, Entry<String, Integer> o2)
            {
                return o2.getValue().compareTo(o1.getValue());
            }
        });

        // Maintaining insertion order with the help of LinkedList
        HashMap<String, Integer> sortedMap = new LinkedHashMap<String, Integer>();
        for (Entry<String, Integer> entry : list)
        {
            sortedMap.put(entry.getKey(), entry.getValue());
        }
        return sortedMap;
    } // End of sortByComparator

	private static void cluster(List<Integer> docsToCluster, int k, int N) 
	{
		int i;
		boolean converged = false;
		List<HashMap<String, Double>> centroids = new ArrayList<>();
		List<Integer> oldCluster = new ArrayList<>();
		for(i=0;i<k;i++)
		{
			centroids.add(new HashMap<String, Double>());
			copyContents( centroids.get(i), hash_TD_outer.get(docsToCluster.get((int)(Math.random()*N))) );
		}
		while (!converged)
		{
			oldCluster.clear();
			for(i=0;i<clusterVector.size();i++)
				oldCluster.add(clusterVector.get(i));
			computeCluser(centroids, docsToCluster);
			centroids = computeCentroids(centroids, docsToCluster, k, N);
			converged = oldCluster.equals(clusterVector);
		}
		
	}

	private static void copyContents(HashMap<String, Double> centroidMap, HashMap<String, Integer> ObjectMap) 
	{	
		for (Map.Entry<String, Integer> entry : ObjectMap.entrySet())
        {
			centroidMap.put(entry.getKey(), entry.getValue()*1.00);
        }
	}

	private static void computeCluser(List<HashMap<String, Double>> centroids, List<Integer> docsToCluster) 
	{
		List<Double> similarities = new ArrayList<>();
		int counter = 0;
		for(int listEntry : docsToCluster)
		{
			for(HashMap<String, Double> entry : centroids)
			{
				similarities.add(computeTfIdfSim(hash_TD_outer.get(listEntry), entry));
			}
			clusterVector.set(counter, similarities.indexOf(Collections.max(similarities)));
			similarities.clear();
			counter++;
		}
	}
	
	private static Double computeTfIdfSim(HashMap<String, Integer> doc, HashMap<String, Double> centroid) 
	{
		Double result = 0.00, docL2 = 0.00, centroidL2 = 0.00;
		double entryValue, centroidValue, currentIDF;
		for(Map.Entry<String, Integer> entry : doc.entrySet())
		{
			entryValue = ((double)(entry.getValue()*1.0));
			centroidValue = centroid.get(entry.getKey()) == null ? 0 : centroid.get(entry.getKey());
			currentIDF = hashIDFVector.get(entry.getKey());
			result += ( entryValue * centroidValue * currentIDF * currentIDF );
			docL2 += ( entryValue * entryValue * currentIDF * currentIDF );
		}
		for(Map.Entry<String, Double> entry : centroid.entrySet())
		{
			centroidL2 += ( entry.getValue() * entry.getValue() * hashIDFVector.get(entry.getKey()) * hashIDFVector.get(entry.getKey()) );
		}
		docL2 = Math.sqrt(docL2);
		centroidL2 = Math.sqrt(centroidL2);
		result = result / (docL2 * centroidL2);
		return result;
	}

	private static List<HashMap<String, Double>> computeCentroids(List<HashMap<String, Double>> oldCentroids, List<Integer> docList, int k, int N) 
	{
		List<HashMap<String, Double>> newCentroids = new ArrayList<>();
		HashMap<String, Integer> docVec = new HashMap<>();
		List<Integer> noOfDoc = new ArrayList<>();
		int lpC, nSize = N, counter, a ;
		
		for(lpC=0; lpC<k; lpC++)
		{
			if(noOfDoc.size() <= lpC)
				noOfDoc.add(0);
			else
				noOfDoc.set(lpC, 0);
			
			if (newCentroids.size() <= lpC)
				newCentroids.add(new HashMap<String, Double>());
			else
				newCentroids.get(lpC).clear();
		}
		
		for(lpC=0; lpC<nSize; lpC++)
		{
			counter = clusterVector.get(lpC);
			noOfDoc.set(counter, noOfDoc.get(counter)+1);
			docVec = hash_TD_outer.get(docList.get(lpC));
			for(Map.Entry<String, Integer> entry : docVec.entrySet())
			{
				a = newCentroids.get(counter).get(entry.getKey()) == null ? 0 : 1;
				if (a==0)
					newCentroids.get(counter).put(entry.getKey(), entry.getValue()*1.0);
				else
					newCentroids.get(counter).put(entry.getKey(), newCentroids.get(counter).get(entry.getKey())+entry.getValue());
			}
		}
		
		for(lpC=0; lpC<nSize; lpC++)
		{
			counter = clusterVector.get(lpC);
			for(Map.Entry<String,Double> entry : newCentroids.get(counter).entrySet())
				entry.setValue(entry.getValue()/noOfDoc.get(counter));
		}
		return newCentroids;
	}
	
	public static String retHtml(List<Integer> docsToCluster, List<String> snippetStrings, String query, int k) throws Exception
	{
		int j;
		Document doc;
		String title,path;
		IndexReader r = IndexReader.open(FSDirectory.open(new File("index")));
		ArrayList<Integer> orderedDocs = new ArrayList<>();
		for (j=0; j<k; j++)
		{
			orderedDocs.add(docsToCluster.get(j));
		}
		
		String html = "<html><head><script source=http://www.public.asu.edu/~vjayabal/olderfiles/assets/bootstrap.js></script>";
		html += "<script source='http://www.public.asu.edu/~vjayabal/olderfiles/assets/jquery-2.1.1.js'></script>";
		html += "<link href='http://www.public.asu.edu/~vjayabal/olderfiles/assets/bootstrap.css' rel='stylesheet'></head>";
		html += "<body bgcolor='#dfdfdf' width='510px'><div class='container-fluid'><h2>Results for " + query + "</h2>";
		for (int i=0; i<k; i++)
		{
			doc = r.document(orderedDocs.get(i));
			path = doc.getFieldable("path").stringValue().replace("%%", "/");
			title = getHtmlTitle("Projectclass/result3/" + doc.getFieldable("path").stringValue());
			html += "<strong>" + title + "</strong><br><small><em><a href='" + path + "'>" + path + "</a></em></small><br>";
			html += "<p class = 'text-muted'><small>" + snippetStrings.get(docsToCluster.indexOf(orderedDocs.get(i))) + "</small></p>";
		}
		html += "</div><br></body></html>";
		r.close();
		return html;
	}
	
	private static String getHtmlTitle(String path) throws Exception
	{
		String content="", line, title="";
		FileReader inputFile = new FileReader(path);
        BufferedReader bufferReader = new BufferedReader(inputFile);
        Pattern p = Pattern.compile("<title>(.*?)</title>|<TITLE>(.*?)</TITLE>|<title>(.*?)</TITLE>|<TITLE>(.*?)</title>");
        while ((line = bufferReader.readLine()) != null)   
        {
        	content += line;
            Matcher m = p.matcher(content);
        	if (m.find() == true)
        	{
        		title = (m.group(1)==null?m.group(2):m.group(1));
        		break;
        	}
        }
        bufferReader.close();
		return title;
	}

	public static String uiInterface(String query, int N, int k)throws Exception
	{
		int cnt=0;
		HashMap<Integer, Double> querySorted;
		List<Integer> docsToCluster = new ArrayList<>();
		List<String> snippetStrings = new ArrayList<>();
		
		querySorted = fetchIdfSimilarity(query);
		for (Map.Entry<Integer, Double> entry : querySorted.entrySet())
		{
			docsToCluster.add(entry.getKey());
//			clusterVector.add(0);
			cnt++;
			if (cnt == N)
				break;
		}
		snippetStrings = generateSnippet(query, docsToCluster, N);
//		cluster(docsToCluster, k, N);
		String retData = retHtml(docsToCluster, snippetStrings, query, k);
		docsToCluster.clear();
//		clusterVector.clear();
		return retData;
	}
	
	private static List<String> clusterStrings(List<Integer> docsToCluster, int k, int N) 
	{
		List<String> clusterKeywords = new ArrayList<>();
		HashMap<String, Integer> wordFreq = new HashMap<>();
		HashMap<String, Integer> docTd;
		int i ,j, a;
		String wordColl;
		
		for(i=0; i<k; i++)
		{
			wordFreq.clear();
			wordColl = "";
			for(j=0; j<N; j++)
			{
				if(clusterVector.get(j) == i)
				{
					docTd = hash_TD_outer.get(docsToCluster.get(j));
					for (Map.Entry<String, Integer> entry : docTd.entrySet())
					{
						a = wordFreq.get(entry.getValue()) == null ? 0 :1 ;
						if( a==0 )
							wordFreq.put(entry.getKey(), entry.getValue());
						else
							wordFreq.put(entry.getKey(), wordFreq.get(entry.getKey()) + entry.getValue() + 20);
					}
				}
			}
			for (Map.Entry<String, Integer> entry : wordFreq.entrySet())
				entry.setValue( (int)(entry.getValue()*hashIDFVector.get(entry.getKey())*hashIDFVector.get(entry.getKey())) );
			
			wordFreq = sortByComp(wordFreq);
			a= 0;
			for(Map.Entry<String, Integer> entry : wordFreq.entrySet())
			{
				wordColl += entry.getKey() + ", ";
				if (++a == 10)
					break;
			}
			clusterKeywords.add(wordColl);
		}
		return clusterKeywords;
	}
	
	private static List<String> generateSnippet(String query, List<Integer> docList, int n)throws Exception 
	{
		int i;
		double bestSim, currentSim;
		String path, line;
		List<String> result = new ArrayList<>();
		List<String> defStr = new ArrayList<>();
		
		Document doc;
		FileReader inputFile;
		BufferedReader bufferReader;
		
		for(i=0;i <n; i++)
		{
			result.add("");
			defStr.add("");
		}
		
		for(i=0; i<n; i++)
		{
			IndexReader r = IndexReader.open(FSDirectory.open(new File("index")));
			doc = r.document(docList.get(i));
			path = doc.getFieldable("path").stringValue();
			inputFile = new FileReader("Projectclass/result3/" + path);
			bufferReader = new BufferedReader(inputFile);
			bestSim = 0.00;
			while ((line = bufferReader.readLine()) != null)  
			{
				line = line.replaceAll("\\<.*?\\>", "");
				line = line.replaceAll("\\&.*?\\;", "");
				if (line.startsWith("<!--"))
					line = line.substring(4, line.length());
				line = line.trim();
				if (!line.isEmpty() && line!=null)
				{
					currentSim = computeSim(line, query);
					if (contains(line,query) && defStr.get(i).isEmpty())
						defStr.set(i,line);
					if (currentSim > bestSim )
					{
						bestSim = currentSim;
						result.set(i, line);
					}
				}
			}
			bufferReader.close();
		}
		for(i=0;i <n; i++)
		{
			if (result.get(i).isEmpty() || result.get(i)==null)
				result.set(i,defStr.get(i));
		}
		
		return result;
	}
	
	private static boolean contains(String line, String query) 
	{
		for (String qWord : query.split(" "))
		{
			if (line.contains(qWord))
				return true;
		}
		return false;
	}

	private static double computeSim(String line, String query) 
	{
		HashMap<String,Integer> lineTF = new HashMap<>(); 
		HashMap<String,Integer> queryTF = new HashMap<>();
		double lineL2Norm = 0.00, queryL2Norm = 0.00, sim = 0.00;

		for (String word : line.split(" "))
		{
			if (!lineTF.containsKey(word))
				lineTF.put(word, 1);
			else
				lineTF.put(word, (lineTF.get(word))+1 );
		}
		
		for (String word : query.split(" "))
		{
			word = word.trim();
			if (!queryTF.containsKey(word))
				queryTF.put(word, 1);
			else
				queryTF.put(word, queryTF.get(word)+1);
		}
		
		for (Map.Entry<String, Integer> entry : lineTF.entrySet())
		{
			double idfV = hashIDFVector.containsKey(entry.getKey())?hashIDFVector.get(entry.getKey()):0;
			lineL2Norm += ((double)(entry.getValue() * entry.getValue()) * idfV * idfV) ;
		}
		lineL2Norm = Math.sqrt(lineL2Norm);
		
		for (Map.Entry<String, Integer> entry : queryTF.entrySet())
		{
			double idfV = hashIDFVector.containsKey(entry.getKey())?hashIDFVector.get(entry.getKey()):0;
			queryL2Norm += ((double)(entry.getValue() * entry.getValue()) * idfV * idfV) ;
		}
		queryL2Norm = Math.sqrt(queryL2Norm);
		
		for (Map.Entry<String, Integer> entry : lineTF.entrySet())
		{
			sim += entry.getValue() * (queryTF.containsKey(entry.getKey())?queryTF.get(entry.getKey()):0);
		}
		sim *= 1.0;
		sim /= lineL2Norm;
		sim /= queryL2Norm;
		
		return sim;
	}

	public static void main(String[] args) throws Exception
	{
		int cnt=0, N=50, k=3;
		String query;
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
		HashMap<Integer, Double> querySorted;
		List<Integer> docsToCluster = new ArrayList<>();
		List<String> snippetStrings;
		List<String> clusterStrings;
		
		System.out.println("PreCompute Starts here");
		init();
		preCompute();
		
		System.out.print("query> ");
		query = in.readLine();
		
		System.out.println("Fetching IDF results");
		querySorted = fetchIdfSimilarity(query);
			
		// -------------------------- SOLUTION TO PROBLEM PART 1 -------------------------------
		System.out.println("Clustring data");
			for (Map.Entry<Integer, Double> entry : querySorted.entrySet())
			{
				docsToCluster.add(entry.getKey());
				clusterVector.add(0);
				cnt++;
				if (cnt == N)
					break;
			}
		cluster(docsToCluster, k, N);
		
		System.out.println("Generating cluseter Specific words");
		clusterStrings = clusterStrings(docsToCluster, k ,N);
//		cnt = 0;
//		for (String str : clusterStrings)
//		{
//			System.out.println("The cluster " + cnt++ + " is characterized by : " + str);
//		}
		
		// ------------------------- SNIPET GENERATION ----------------------------------------
		
		System.out.println("Generating Snippet");
		snippetStrings = generateSnippet(query, docsToCluster, N);
//		for (String str : snippetStrings)
//		{
//			System.out.println("The doc " + cnt++ + " is rep by : " + str);
//		}
	}
}