package com.mike.samples;
/*
 * Created on Feb 15, 2005
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
// package com.mike.swinger;

import java.io.* ;
import java.util.ArrayList;
import java.util.logging.*;
import java.util.concurrent.ThreadLocalRandom;

import com.ibm.json.java.JSONArray;
import com.ibm.json.java.JSONObject;

/**
 * GenTestData reads a JSON file with this rough format (very little error checking at this point):
 {"OutForm" : "JSON", "NumRows" : 100, "ColArray" : [
   { "Name": "CustID", "Type": "Integer", "RangeMin" : 10000.0, "RangeMax" : 60000.0, "RangeIncrement" : 1.0 },
   { "Name": "FirstName", "Type": "FirstName", "Selection" : "Sequential" },
   { "Name": "LastName", "Type": "LastName" },
   { "Name": "Department", "Type" : "StringList", "Selection" : "Random", "Values" : [
        "E725", "E325", "S295", "S595" ]},
   { "Name": "Age", "Type": "Integer", "RangeMin" : 19.0, "RangeMax": 99.0, "Selection": "Random" },
   { "Name": "Salary", "Type": "Double", "Precision" : 2, "RangeMin" : 10000.0, "RangeMax" : 1000000.0,
   	"Selection": "Random" },
   { "Name": "StreetAddr", "Type" : "Street", "HousesPerStreet" : 20, "MinHouseNum": 1111 },
   { "Name": "City", "Type": "City", "Selection": "Random" },
   { "Name": "State", "Type": "State"},
   { "Name": "ZipCode", "Type" : "ZipCode" } ] }
 *   
 *   A foreign key type of file can use an existing file and generate from that.  For example, it can create
 *   Accounts for all the CustIDs.  All new columns follow above order, but key is as follows (Accounts data)
{"OutForm" : "JSON", "RowsPerKey" : 3, "ForeignFile" : "/Users/Michael/misc/data/json1.generatedData.dat", "ColArray" : [
   { "Name": "CustID", "Type": "ForeignKey", "ForeignKey" : "CustID", "ForeignType": "Integer" },
   { "Name": "AcctId", "Type": "Integer", "RangeMin" : 101.0, "RangeMax" : 999.0, "Selection": "Random" },
   { "Name": "AcctType", "Type" : "StringList", "Selection" : "Random", "Values" : [ "CH", "SA", "CL", "ML" ]},
   { "Name": "AcctBal", "Type" : "Double", "Precision" : 2, "RangeMin" : 50.0, "RangeMax" : 10000.0,
      "Selection" : "Random", "IFCOND" : { "ColCompare" : "AcctType", "Operator" : "EQUAL", "OP1" : "CH" }},
   { "Name": "AcctBal", "Type" : "Double", "Precision" : 2, "RangeMin" : 50.0, "RangeMax" : 10000.0,
      "Selection" : "Random", "IFCOND" : { "ColCompare" : "AcctType", "Operator" : "EQUAL", "OP1" : "SA" }},
   { "Name": "AcctBal", "Type" : "Double", "Precision" : 2, "RangeMin" : 500.0, "RangeMax" : 30000.0,
      "Selection" : "Random", "IFCOND" : { "ColCompare" : "AcctType", "Operator" : "EQUAL", "OP1" : "CL" }},
   { "Name": "AcctBal", "Type" : "Double", "Precision" : 2, "RangeMin" : 5000.0, "RangeMax" : 300000.0,
      "Selection" : "Random", "IFCOND" : { "ColCompare" : "AcctType", "Operator" : "EQUAL", "OP1" : "ML" }}
  ]}
    
 *   Current Types: FirstName, LastName, ForeignKey, Integer, Double, Street, City, State, ZipCode, StringList.
 *   Current OutForms: CSV, JSON
 *   RangeIncrement defaults to 1
 *   Selection defaults to Sequential (optional value "Random")
 *   State and Zip Code will do what City does (ie: State and zip follow the city)
 *   IFCOND is simple IF condition (expandable if needed later) where ... if true, this is included
 *     OPERATOR EQUAL/OP1 for strings and BETWEEN/OP1/OP2 for numeric (for one val, make OP1/OP2 same)
 *     ColCompare must be before this column
 *     Can put in multiple instances of same column so long as <= 1 is true
 *   Data files for FirstName, LastName, Streets, Towns, and Zip 
 *     must be in the jar with this class in the data directory
 */
public class GenTestData {
	private static final String thisClass = GenTestData.class.getName() ;
	private static Logger gtdLogger = Logger.getLogger(thisClass) ;
	private static final String DataOutName = "generatedData.dat" ;
	private static final String [] OutTypes = { "CSV", "JSON" } ;
	private static final String [] TypesOTypes = { "FirstName", "LastName", "Integer", "Double",
		"Street", "City", "State", "ZipCode", "ForeignKey", "StringList"} ;
	private static final String [] ForeignTypes = { "String", "Integer", "Double" } ;
	private static ClassLoader cLoader ;
	private static String [] firstNameList = null, lastNameList = null, streetList = null, cityList = null,
		stateList = null, zipList = null ;
	/**
	 * Information held on the optional IF clauses associated with each column entry
	 */
	private class IfInfo {
		String colCompare ;
		int  cOperator ;		// 0 = EQUAL, 1 = BETWEEN
		String cOp1 ;
		double nOp1 ;
		double nOp2 ;
		
		IfInfo(String colCompare, int cOperator, String cOp1, double nOp1, double nOp2) {
			this.colCompare = colCompare ;
			this.cOperator = cOperator ;
			this.cOp1 = cOp1 ;
			this.nOp1 = nOp1 ;
			this.nOp2 = nOp2 ;
		}
	}
	
	/**
	 * Info on each column being generated in the output 
	 */
	private class ColInfo {
		String name ;
		int cType ;		// per order in TypesOTypes array 
		double rangeMin ;
		double rangeMax ;
		int dSelection ;  // 0 = sequential, 1 = random
		int cPrecision ;
		int minHouseNum ;
		String [] stringList ;
		String  foreignKey ;
		int foreignType ;
		IfInfo ifInfo = null ;
		
		ColInfo(String name, int cType, double rangeMin, double rangeMax, int dSelection, int cPrecision,
			int minHouseNum, String [] stringList, String foreignKey, String foreignType, IfInfo ifInfo) {
			this.name = name ;
			this.cType = cType ;
			this.rangeMin = rangeMin ;
			this.rangeMax = rangeMax ;
			this.dSelection = dSelection ;
			this.cPrecision = cPrecision ;
			this.minHouseNum = minHouseNum ;
			this.stringList = stringList ;
			this.foreignKey = foreignKey ;
			this.foreignType = (foreignType == null) ? -1 : getTypeIdx(foreignType, ForeignTypes) ;
			this.ifInfo = ifInfo ;
		}
	}

	/**
	 * static Main method for generating a single output file
	 * @param args 0 = output directory, 1 = JSON config input
	 */
	public static void main(String args[]) {
		final String METHOD = "main" ;
		int outType = 0 ;		// Dflt is CSV
		GenTestData gtd = new GenTestData() ;
		try {
			cLoader = Class.forName("com.mike.samples.GenTestData").getClassLoader() ;	// Classloader for getting data as stream
		} catch (ClassNotFoundException e1) {
			e1.printStackTrace();
		}
		gtdLogger.logp(Level.INFO, thisClass, METHOD, "Into GenTestData with output target: {0} and JSONCfg: {1}",
			new Object [] { args[0], args[1] });
		String outDirStr = args[0], jsonPropStr = args[1] ;
		JSONObject cfgJson = new JSONObject() ;
		BufferedWriter bw = null ;
		try {		// Open output for writing
			bw = new BufferedWriter(new FileWriter(outDirStr+"/"+DataOutName)) ;
			cfgJson = JSONObject.parse(new FileInputStream(new File(jsonPropStr))) ;
		} catch (FileNotFoundException e) {
			gtdLogger.logp(Level.WARNING, thisClass, METHOD, "FileNotFound exception on writer or json parse: {0}",	e);
		} catch (IOException e) {
			gtdLogger.logp(Level.WARNING, thisClass, METHOD, "IO exception on writer or json parse: {0}",	e);
		}
		outType = gtd.getTypeIdx((String)cfgJson.get("OutForm"), OutTypes) ;
		JSONArray dataEls = (JSONArray)cfgJson.get("ColArray") ;	// Retrieve col info from JSON object
		Long numRows = (Long)cfgJson.get("NumRows") ;
		
		ColInfo [] colInfo = gtd.getColInfo(dataEls) ;
		int [] colIdxs = new int[colInfo.length] ;			// Hold current index for each column (may not all be needed)
		for (int i = 0; i < colIdxs.length; i++)  colIdxs[i] = -1 ;		// Set them all for next bump
		if (numRows != null) {
			for (int i = 0; i < numRows; i++) {
				gtd.processOneRow(bw, colInfo, colIdxs, null) ;
			}
		} else {
			gtd.processFromForeign(((Long)cfgJson.get("RowsPerKey")).intValue(),
				(String)cfgJson.get("ForeignFile"), bw, colIdxs, colInfo) ;
		}
		try {
			bw.close();
		} catch (IOException e) {
			gtdLogger.logp(Level.WARNING, thisClass, METHOD, "IO exception closing bufferedWriter: {0}",	e);
		}
	}
	
	/**
	 * Handle generation of a data file that has as its primary key (or part there-of) a different file with a foreign key
	 * to this file.  It reads thru the other file and generates one or more rows for each key
	 * @param rowsPerKey How many rows should be created for each foreign-key value
	 * @param foreignFile Foreign file itself so it can be read
	 * @param bw BufferedWriter for the output
	 * @param dataEls JSONArray of each column to be created in each row
	 */
	private void processFromForeign(int rowsPerKey, String foreignFile, BufferedWriter bw, int [] colIdxs, ColInfo [] colInfo) {
		final String METHOD = "processFromForeign" ;
		BufferedReader br = null ;
		try {
			br = new BufferedReader(new FileReader(new File(foreignFile))) ;
			String ln ;
			while ((ln = br.readLine()) != null) {
				JSONObject foreignLn = new JSONObject() ;
				foreignLn = JSONObject.parse(ln) ;
				for (int i = 0; i < rowsPerKey; i++) {
					processOneRow(bw, colInfo, colIdxs, foreignLn) ;
				}
			}
		} catch (FileNotFoundException e) {
			gtdLogger.logp(Level.WARNING, thisClass, METHOD, "Exception in processing loop: {0}", e);
		} catch (IOException e) {
			gtdLogger.logp(Level.WARNING, thisClass, METHOD, "Exception in reading foreign file: {0}", e);
		}
	}

	/**
	 * Generate a specific row 
	 * @param bw
	 * @param dataEls
	 * @param foreignRow
	 */
	// TODO: Precision for doubles
	private void processOneRow(BufferedWriter bw, ColInfo [] colInfo, int [] colIdxs, JSONObject foreignRow) {
		final String METHOD = "processOneRow" ;
		int locIdx = -1 ;		// If there is a need for a shared idx like city/state/zip
		JSONObject curRow = new JSONObject() ;
		int curIdx = -1 ;
		Double iDouble = null ;								// Current double value (numeric calculations et al)
		String hString = null ;
		for (ColInfo curCol : colInfo) {
			curIdx++ ;
			if (curCol.ifInfo != null && checkIfInfo(curCol, curRow) == false)  continue ;
			switch (curCol.cType) {
			case(0): // FirstName
				hString = getStringFromList(firstNameList, curCol.dSelection, colIdxs, curIdx) ;
				curRow.put(curCol.name, hString) ;
				break ;
			case(1): // LastName
				hString = getStringFromList(lastNameList, curCol.dSelection, colIdxs, curIdx) ;
				curRow.put(curCol.name, hString) ;
				break ;
			case(2): // Integer
				iDouble = getNumericValue(curCol.dSelection, curCol.rangeMin, curCol.rangeMax, colIdxs, curIdx) ;
				curRow.put(curCol.name, iDouble.intValue()) ;
				break ;
			case(3): // Double
				iDouble = getNumericValue(curCol.dSelection, curCol.rangeMin, curCol.rangeMax, colIdxs, curIdx) ;
				curRow.put(curCol.name, iDouble) ;
				break ;
			case(4): // Street
				hString = getStringFromList(streetList, curCol.dSelection, colIdxs, curIdx) ;
				int streetNum = curCol.minHouseNum + ThreadLocalRandom.current().nextInt(100, 9999) ;
				curRow.put(curCol.name, ((Integer)streetNum).toString()+" "+hString) ;
				break ;
			case(5): // City
				if (locIdx < 0) {
					hString = getStringFromList(cityList, curCol.dSelection, colIdxs, curIdx) ;
					locIdx = colIdxs[curIdx] ;
				} else
					hString = cityList[locIdx] ;
				curRow.put(curCol.name, hString) ;
				break ;
			case(6): // State
				if (locIdx < 0) {
					hString = getStringFromList(stateList, curCol.dSelection, colIdxs, curIdx) ;
					locIdx = colIdxs[curIdx] ;
				} else
					hString = stateList[locIdx] ;
				curRow.put(curCol.name, hString) ;
				break ;
			case(7): // Zips
				if (locIdx < 0) {
					hString = getStringFromList(zipList, curCol.dSelection, colIdxs, curIdx) ;
					locIdx = colIdxs[curIdx] ;
				} else
					hString = zipList[locIdx] ;
				curRow.put(curCol.name, hString) ;
				break ;
			case(8):	// ForeignKey
				Object fKey = foreignRow.get(curCol.foreignKey) ;
				int fType = curCol.foreignType ;
				switch(fType) {
				case(0):		// String
					curRow.put(curCol.name, (String)fKey) ;  break ;
				case(1):		// Integer
					curRow.put(curCol.name, fKey) ; break ;
				case(2):		// Double
					curRow.put(curCol.name, fKey) ; break ;
				}
				break ;
			case(9):	// StringList
				hString = getStringFromList(curCol.stringList, curCol.dSelection, colIdxs, curIdx) ;
				curRow.put(curCol.name, hString) ;
				break ;
			
			}
		}
		try {
			bw.write(curRow.serialize()+"\n") ;
		} catch (IOException e) {
			gtdLogger.logp(Level.WARNING, thisClass, METHOD, "IOException in writing JSON. JSON Content: {0}  Exception: {1}",
				new Object [] { curRow, e });
		}
	}
	
	/**
	 * current row has an IF clause ... return the true/false value of the execution of that if
	 * @param colInfo Current column info (including IF clause info)
	 * @param curRow Values already attained in current row against which IF clause may pull a value
	 * @return true or false based on the comparison(s)
	 */
	private boolean checkIfInfo(ColInfo colInfo, JSONObject curRow) {
		final String METHOD = "checkIfInfo" ;
		IfInfo ifInfo = colInfo.ifInfo ;
		if (ifInfo.cOperator == 0) {		// String equality
			String colCompare = (String)curRow.get(ifInfo.colCompare) ;
			if (colCompare == null) {
				gtdLogger.logp(Level.INFO, thisClass, METHOD, "Tried comparing to: {0} but that column not found", ifInfo.colCompare);
				return false ;
			}
			return colCompare.equals(ifInfo.cOp1) ;
		} else {							// Numeric between (range)
			Object numCompareO = curRow.get(ifInfo.colCompare) ;
			if (numCompareO == null) {
				gtdLogger.logp(Level.INFO, thisClass, METHOD, "Tried comparing to: {0} but that column not found", ifInfo.colCompare);
				return false ;
			}
			Double numCompare = (Double)numCompareO ;		// 2 steps to easily identify possible casting issue
			return (numCompare >= ifInfo.nOp1 && numCompare <= ifInfo.nOp2) ;
		}
	}
	
	/**
	 * retrieve info from the JSON object for each column in output and store the data appropriately.  Each column
	 * is stored in an entry in a JSON array.
	 * @param dataEls The array containing the JSON defining each column.
	 * @return
	 */
	private ColInfo [] getColInfo(JSONArray dataEls) {
		final String METHOD = "getColInfo" ;
		ColInfo [] colArr = new ColInfo[1] ;
		ArrayList<ColInfo>  colList = new ArrayList<GenTestData.ColInfo>() ;
		for (Object curColO : dataEls) {
			JSONObject curCol = (JSONObject)curColO ;		// Iterate thru the entries in the JSONArray
			String cName = (String)curCol.get("Name") ;		// Pull the column name
			int cType = getTypeIdx((String)curCol.get("Type"), TypesOTypes) ;	// Get type into an integer
			if (cType < 0) {								// Invalid type
				gtdLogger.logp(Level.WARNING, thisClass, METHOD, "Invalid column type: {0} for col: {1}",
					new Object [] { (String)curCol.get("Type"), cName });
				continue ;
			}
					// If selection from file or among choices is taken, default is sequential (0), otherwise random
			int dSelection = ("Random".equals((String)curCol.get("Selection"))) ? 1 : 0 ;
			IfInfo ifInfo = getIfInfo(curCol) ;		// If there is an IF clause on column, capture it
			ColInfo colInfo = null ;				// Column info object for this column
			switch (cType) {						// Different processing for different types of columns
			case(0): // FirstName
				if (firstNameList == null)  loadFirstNames() ;
				colInfo = new GenTestData.ColInfo(cName, cType, 0, 0, dSelection, 0, 0, null, null, null, ifInfo) ;
				break ;
			case(1): // LastName
				if (lastNameList == null)  loadLastNames() ;
				colInfo = new GenTestData.ColInfo(cName, cType, 0, 0, dSelection, 0, 0, null, null, null, ifInfo) ;
				break ;
			case(2): // Integer
				colInfo = new GenTestData.ColInfo(cName, cType, (Double)curCol.get("RangeMin"),
					(Double)curCol.get("RangeMax"),	dSelection, 0, 0, null, null, null, ifInfo) ;
				break ;
			case(3): // Double
				colInfo = new GenTestData.ColInfo(cName, cType, (Double)curCol.get("RangeMin"),
					(Double)curCol.get("RangeMax"), dSelection, ((Long)curCol.get("Precision")).intValue(),
					0, null, null, null, ifInfo) ;
				break ;
			case(4): // Street
				if (streetList == null)  loadStreets() ;
				colInfo = new GenTestData.ColInfo(cName, cType, 0, 0, dSelection, 0,
					((Long)curCol.get("MinHouseNum")).intValue(), null,	null, null, ifInfo) ;
				break ;
			case(5): // City
				if (cityList == null)  loadTowns() ;
				colInfo = new GenTestData.ColInfo(cName, cType, 0, 0, dSelection, 0, 0, null, null, null, ifInfo) ;
				break ;
			case(6): // State
				if (cityList == null)  loadTowns() ;	// Towns has city and state
				colInfo = new GenTestData.ColInfo(cName, cType, 0, 0, dSelection, 0, 0, null, null, null, ifInfo) ;
				break ;
			case(7): // Zips
				if (zipList == null)  loadZipCodes() ;
				colInfo = new GenTestData.ColInfo(cName, cType, 0, 0, dSelection, 0, 0, null, null, null, ifInfo) ;
				break ;
			case(8):	// ForeignKey
				colInfo = new GenTestData.ColInfo(cName, cType, 0, 0, dSelection, 0, 0, null,
					(String)curCol.get("ForeignKey"), (String)curCol.get("ForeignType"), ifInfo) ;
				break ;
			case(9):	// StringList
				String [] listVals = getStringVals((JSONArray)curCol.get("Values")) ;
				colInfo = new GenTestData.ColInfo(cName, cType, 0, 0, dSelection, 0, 0, listVals,
					null, null, ifInfo) ;
				break ;
			}
			colList.add(colInfo) ;
		}
		colArr = colList.toArray(colArr) ;
		return colArr ;
	}
		
	/**
	 * Get type of column from array and thus convert to int index
	 * @param jType Column type from JSON
	 * @return int idx for this column type
	 */
	private int getTypeIdx(String jType, String [] array2Search) {
		for (int i = 0; i < array2Search.length; i++) {
			if (jType.equals(array2Search[i])) return i ;
		}
		return -1 ;
	}
		
	/**
	 * See if this column has an IF clause.  If so, capture it
	 * @param curCol JSON object with info for this column
	 * @return The IfInfo object built from the JSON
	 */
	private IfInfo getIfInfo(JSONObject curCol) {
		JSONObject ifStruc = (JSONObject)curCol.get("IFCOND") ;
		if (ifStruc == null) return null ;		// No if for this column
		String colCompare = (String)ifStruc.get("ColCompare") ;
		String cOp1 = null ;  Double nOp1 = 0.0 , nOp2 = 0.0 ;
		int cOperator = "BETWEEN".equals((String)ifStruc.get("Operator")) ? 1 : 0 ;
		if (cOperator == 0) {		// Default, string equals
			cOp1 = (String)ifStruc.get("OP1") ;
		} else {
			nOp1 = (Double)ifStruc.get("OP1") ;
			nOp2 = (Double)ifStruc.get("OP2") ;
		}
		return new IfInfo(colCompare, cOperator, cOp1, nOp1, nOp2) ;
	}
	
	/**
	 * Load the firstNamesList String array with firstNames from file included in jar w/class
	 */
	private void loadFirstNames() {
		firstNameList = loadGenericStrings("data/firstNames.list") ;
	}
	
	/**
	 * Load the lastNamesList String array with firstNames from file included in jar w/class
	 */
	private void loadLastNames() {
		lastNameList = loadGenericStrings("data/lastNames.list") ;
	}

	private void loadStreets() {
		streetList = loadGenericStrings("data/streetNames.list") ;
	}
	
	private void loadTowns() {
		String [] tempList = loadGenericStrings("data/towns.list") ;
		cityList = new String[tempList.length] ;
		stateList = new String[tempList.length] ;
		for (int i = 0; i < tempList.length; i++) {
			String [] locations = tempList[i].split(", ", 2) ;
			cityList[i] = locations[0] ;
			stateList[i] = locations[1] ;
		}
	}

	private void loadZipCodes() {
		zipList = loadGenericStrings("data/zip.list") ;
	}
	
	/**
	 * Load an array based on an input stream
	 */
	private String [] loadGenericStrings(String resourceId) {
		final String METHOD = "loadGenericStrings" ;
		String ln ;
		ArrayList<String> nameList = new ArrayList<String>() ;
		String [] outList = new String [1] ;
		InputStream is = cLoader.getResourceAsStream(resourceId) ;
		BufferedReader br = new BufferedReader(new InputStreamReader(is)) ;
		try {
			while ((ln = br.readLine()) != null) {
				nameList.add(ln) ;
			}
			br.close();
		} catch (IOException e) {
			gtdLogger.logp(Level.WARNING, thisClass, METHOD, "IOException reading stringFile: {0}",	e);
		}
		outList = nameList.toArray(outList) ;
		return outList ;
	}

	/**
	 * Return a string from a list and, if applicable, update the stored offset for the column (sequential)
	 * @param sList	   Array of strings from which to extract
	 * @param selectTp	Select randomly or sequentially
	 * @param offsetArr	 Array of offsets for all the columns (allows this method to update offset)
	 * @param offsetIdx  Offset with offsetArr pertaining to this column
	 * @return  String from sList randomly or sequentially selected
	 */
	private String getStringFromList(String [] sList, int selectTp, int [] offsetArr, int offsetIdx) {
		int newIdx = 0 ;
		if (selectTp == 0) {		// Default sequential thru list
			newIdx = offsetArr[offsetIdx] + 1 ;
			if (newIdx >= sList.length)  newIdx = 0 ;
		} else {
			newIdx = ThreadLocalRandom.current().nextInt(0, sList.length) ;
		}
		offsetArr[offsetIdx] = newIdx ;
		return sList[newIdx] ;
	}

	/**
	 * Return a numeric value w/in a range (sequential or random)
	 * @param selectTp  Sequential (ie: return next value per last row) or random
	 * @param rangeMin  Minimum number
	 * @param rangeMax  Maximum number
	 * @param offsetArr	 Array of offsets for all the columns (allows this method to update offset)
	 * @param offsetIdx  Offset with offsetArr pertaining to this column
	 * @return
	 */
	private double getNumericValue(int selectTp, double rangeMin, double rangeMax, int [] offsetArr, int offsetIdx) {
		int newIdx = 0 ;
		if (selectTp == 0) {		// Sequential
			newIdx = offsetArr[offsetIdx] + 1 ;
			if (newIdx >= rangeMax)  newIdx = ((Double)rangeMin).intValue() ;
			offsetArr[offsetIdx] = newIdx ;
			return newIdx ;
		} else {
			return Math.floor((ThreadLocalRandom.current().nextDouble(rangeMin, rangeMax+1)) * 100) / 100 ;
		}
	}

	private String [] getStringVals(JSONArray jArray) {
		String [] retStrings = new String [jArray.size()] ;
		int i = 0 ;
		for (Object vObj : jArray) {
			retStrings[i++] = (String)vObj ;
		}
		return retStrings ;
	}
}