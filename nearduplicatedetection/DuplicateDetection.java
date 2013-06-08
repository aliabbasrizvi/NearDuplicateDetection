package nearduplicatedetection;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeMap;

public class DuplicateDetection {
	HashMap<String, Integer> shingleLabel;
	ArrayList<String> docs;
	TreeMap<String, ArrayList<Integer>> documentShingles;
	TreeMap<String, ArrayList<Integer>> documentSketch;
	int totalDocuments, shingleCount, p;
	int a[], b[];
	final int randomValueCount = 25;
	final double thresholdVal = 0.5;
	final String whiteSpacePattern = "\\s";

	public DuplicateDetection() {
		shingleLabel = new HashMap<String, Integer>();
		documentShingles = new TreeMap<String, ArrayList<Integer>>();
		documentSketch = new TreeMap<String, ArrayList<Integer>>();
		docs = new ArrayList<String>();
		totalDocuments = 0;
		shingleCount = 0;
		a = new int[randomValueCount];
		b = new int[randomValueCount];	
	}

	/**
	 * Function to iterate over each file in the directory
	 * 
	 * @param dirPath       Directory path where all documents are stored
	 * @throws IOException  
	 */
	public void parseFileDirectory(String dirPath) throws IOException {
		File dir = new File(dirPath);
		for (File file : dir.listFiles()) {
			if (file.getName().equals(".") ||
					file.getName().equals("..") || 
					file.isHidden() || 
					file.isDirectory())
				continue;

			parseFile(file.getAbsolutePath(), file.getName());
			docs.add(file.getName());
			totalDocuments++;
		}
		p = getClosestPrimeNumber(shingleCount);
		generateABValues();
	}

	/**
	 * Function to parse over a file's content and determine all the shingles
	 * 
	 * @param filePath		Path to the file which is to be parsed
	 * @param fileName		File's name
	 * @throws IOException
	 */
	private void parseFile(String filePath, String fileName) throws IOException {
		FileInputStream fstream = null;
		String line, fileContent = "";

		documentShingles.put(fileName, new ArrayList<Integer>());
		try {
			fstream = new FileInputStream(filePath);
		} catch (FileNotFoundException e) {
			System.out.println("File does not exist. Set correct path");
		}

		DataInputStream in = new DataInputStream(fstream);
		BufferedReader br = new BufferedReader(new InputStreamReader(in));

		while((line = br.readLine()) != null) 
			fileContent+=line;

		String fileWords[] = fileContent.split(whiteSpacePattern);
		for (int wordCount = 0; wordCount < fileWords.length - 2; wordCount++) {
			String shingle = fileWords[wordCount].toLowerCase() + " " +
					fileWords[wordCount+1].toLowerCase() + " " +
					fileWords[wordCount+2].toLowerCase();
			if (!shingleLabel.containsKey(shingle)) {
				shingleLabel.put(shingle, shingleCount++);
			}
			if (!documentShingles.get(fileName).contains(shingleLabel.get(shingle)))
				documentShingles.get(fileName).add(shingleLabel.get(shingle));
		}
	}

	/**
	 * Function to determine next prime number after the number of shingles
	 * 
	 * @param count	Number of shingles in the document space
	 * @return		Closest prime number following count
	 */
	private int getClosestPrimeNumber(int count) {
		int primeNum, flag = 0;
		if (count % 2 == 0)
			count = count + 1;

		for (primeNum = count; flag == 0; primeNum+=2) {
			for (int i = 3; i < Math.sqrt(primeNum) && flag == 0; i++)
				if (primeNum % i == 0)
					flag = 1;
			
			if (flag == 0)
				flag = 1;
			else
				flag = 0;
		}

		return (primeNum - 2);	
	}

	/**
	 * Function to generate random a, b values as required
	 */
	private void generateABValues() {
		for (int i = 0; i < randomValueCount; i++) {
			a[i] = 1 + (int)(Math.random() * (p - 1));
			b[i] = (int)(Math.random() * p);
		}
	}

	/**
	 * Function to generate the document's sketch as per the a, b values
	 */
	public void generateDocumentSketch() {
		int s, shingleCount, minVal, f, minShingle;
		for (String document : documentShingles.keySet()) {
			documentSketch.put(document, new ArrayList<Integer>());
			for (s = 0; s < randomValueCount; s++) {
				minVal = p + 1;
				minShingle = p + 1;
				for (shingleCount = 0; shingleCount < documentShingles.get(document).size(); shingleCount++) {
					f = (a[s]*documentShingles.get(document).get(shingleCount) + b[s])%p;
					if (f < minVal) {
						minVal = f;
						minShingle = documentShingles.get(document).get(shingleCount);
					}
				}
				documentSketch.get(document).add(minShingle);
			}
		}
	}

	/**
	 * Function to compute estimated Jaccard coefficient for documents doc1, doc2
	 * 
	 * @param doc1	Name of a document
	 * @param doc2	Name of another document
	 * @return		Estimated Jaccard coefficient
	 */
	private double estimatedJaccardCoefficient(String doc1, String doc2) {
		int matchingShingles = 0;
		int doc1Index = 0;

		ArrayList<Integer> doc1Sketch = documentSketch.get(doc1);
		ArrayList<Integer> doc2Sketch = documentSketch.get(doc2);

		for (doc1Index = 0; doc1Index < doc1Sketch.size(); doc1Index++) {
			if (doc2Sketch.contains(doc1Sketch.get(doc1Index)))
				matchingShingles++;
		}

		return ((double)matchingShingles/(double)randomValueCount);
	}

	/**
	 * Function to compute actual Jaccard coefficient for documents doc1, doc2
	 * 
	 * @param doc1	Name of a document
	 * @param doc2	Name of another document
	 * @return		Actual Jaccard coefficient
	 */
	private double actualJaccardCoefficient(String doc1, String doc2) {
		ArrayList<Integer> uniqueShingles = new ArrayList<Integer>();
		int matchingShingles = 0;
		int i;

		for (i = 0; i < documentShingles.get(doc1).size(); i++)
			uniqueShingles.add(documentShingles.get(doc1).get(i));

		for (i = 0; i < documentShingles.get(doc2).size(); i++)
			if (!uniqueShingles.contains(documentShingles.get(doc2).get(i)))
				uniqueShingles.add(documentShingles.get(doc2).get(i));
			else
				matchingShingles++;

		return ((double)matchingShingles/(double)uniqueShingles.size());
	}

	/**
	 * Function to generate pairs of documents that satisfy the minimum Jaccard coefficient 
	 * threshold value. Also displays actual Jaccard coefficient for those documents.
	 * 
	 * @return  Boolean value denoting if a pair was found or not
	 */
	public boolean generateDocumentPairs() {
		int doc1, doc2;
		boolean foundPair = false;
		for (doc1 = 0; doc1 < docs.size() - 1; doc1++) {
			for (doc2 = doc1 + 1; doc2 < docs.size(); doc2++) {
				if (estimatedJaccardCoefficient(docs.get(doc1), docs.get(doc2)) > thresholdVal) {
					System.out.println(docs.get(doc1) + " " + docs.get(doc2));
					System.out.println("Estimated Jaccard coefficient = " + estimatedJaccardCoefficient(docs.get(doc1), docs.get(doc2)));
					System.out.println();
					foundPair = true;
				}
			}
		}
		return foundPair;
	}

	/**
	 * Displays top matching documents i.e. having high Jaccard coefficient
	 * 
	 * @param numberOfDocuments	Number of documents from the starting for which top matching documents are to be found
	 * @param topCount			Number of top documents to be displayed
	 */
	public void generateTopDocuments(int numberOfDocuments, int topCount) {
		if (topCount <= 0) {
			System.out.println("Invalid parameters passed. Not generating top documents. Exiting.");
			System.exit(1);
		}
		int doc1, doc2, top, topDoc;
		double max;
		double topCoeff[][] = new double[numberOfDocuments][docs.size()];
		System.out.println("Displaying top " + topCount	+ " Jaccard Coefficient documents for first " + numberOfDocuments + " documents\n");
		for (doc1 = 0; doc1 < numberOfDocuments && doc1 < docs.size(); doc1++) {
			for (doc2 = 0; doc2 < docs.size(); doc2++) {
				if (doc1 == doc2) {
					continue;
				} else {
					topCoeff[doc1][doc2] = estimatedJaccardCoefficient(docs.get(doc1), docs.get(doc2));
				}
			}
		}

		for (doc1 = 0; doc1 < numberOfDocuments && doc1 < numberOfDocuments; doc1++) {
			System.out.println((doc1 + 1) + "." + docs.get(doc1));
			System.out.println("Top documents are: ");
			for (top = 0; top < topCount; top++) {
				max = -1;
				topDoc = -1;
				for (doc2 = 0; doc2 <docs.size(); doc2++) {
					if (topCoeff[doc1][doc2] > max) {
						max = topCoeff[doc1][doc2];
						topDoc = doc2;
					}
				}
				System.out.println((top + 1) + "." + docs.get(topDoc));
				System.out.println("Jaccard Coefficient = " + max);
				topCoeff[doc1][topDoc] = -1;
			}
			System.out.println();
		}
	}

	public static void main(String[] args) throws IOException {
		DuplicateDetection gs = new DuplicateDetection();
		if (args.length != 1) {
			System.out.println("Incorrect number of parameters entered. EXITING.");
			System.exit(1);
		}
		gs.parseFileDirectory(args[0]);
		gs.generateDocumentSketch();
		if (!gs.generateDocumentPairs())
			System.out.println("No document pair found which satisfies the Jaccard coefficient threshold.");
		gs.generateTopDocuments(10, 3);
	}
}
