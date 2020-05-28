package jadd;

import java.util.Map;

import org.bridj.IntValuedEnum;
import org.bridj.Pointer;

import bigcudd.BigcuddLibrary;
import bigcudd.BigcuddLibrary.Cudd_ReorderingType;
import bigcudd.BigcuddLibrary.Dddmp_VarInfoType;
import bigcudd.BigcuddLibrary.Dddmp_VarMatchType;
import bigcudd.DdNode;

/**
 * Interface to basic ADD operations.
 *
 * @author thiago
 *
 */
public class JADD {

    private Pointer<BigcuddLibrary.DdManager> dd;
    private VariableStore variableStore = new VariableStore();

    public JADD() {
        dd = BigcuddLibrary.Cudd_Init(0,
                                      0,
                                      BigcuddLibrary.CUDD_UNIQUE_SLOTS,
                                      BigcuddLibrary.CUDD_CACHE_SLOTS,
                                      0);
        IntValuedEnum<Cudd_ReorderingType> method = Cudd_ReorderingType.CUDD_REORDER_SYMM_SIFT;
//        BigcuddLibrary.Cudd_AutodynEnable(dd, method);
    }

    public ADD makeConstant(double constant) {
        return new ADD(dd,
                       BigcuddLibrary.Cudd_addConst(dd,  constant),
                       variableStore);
    }

    public ADD getVariable(String varName) {
        if (variableStore.contains(varName)) {
            return variableStore.get(varName);
        } else {
            Pointer<DdNode> var = BigcuddLibrary.Cudd_addNewVar(dd);
            ADD varADD = new ADD(dd, var, variableStore);
            variableStore.put(var.get().index(), varName, varADD);
            return varADD;
        }
    }

    /**
    * Performs an optimal reordering of the variables for the managed ADDs
    * based on the sifting heuristic.
    */
    public void reorderVariables() {
        IntValuedEnum<Cudd_ReorderingType> heuristic = Cudd_ReorderingType.CUDD_REORDER_SYMM_SIFT;
        BigcuddLibrary.Cudd_ReduceHeap(dd, heuristic, 1);
    }

    /**
     * Manually adjusts variables ordering to mimic that of the
     * {@code orderedVariables} array.
     * @param orderedVariables
     * @throws UnrecognizedVariableException
     */
    public void setVariableOrder(String[] orderedVariables) throws UnrecognizedVariableException {
        int[] permutationVector = variableStore.toPermutationVector(orderedVariables);
        BigcuddLibrary.Cudd_ShuffleHeap(dd, Pointer.pointerToInts(permutationVector));
    }

    public void dumpDot(String[] functionNames, ADD[] functions, String fileName) {
        Pointer<?> output = CUtils.fopen(fileName, CUtils.ACCESS_WRITE);

        @SuppressWarnings("unchecked")
        Pointer<DdNode>[] nodes = (Pointer<DdNode>[]) new Pointer[functions.length];
        int i = 0;
        for (ADD function : functions) {
            nodes[i] = function.getUnderlyingNode();
            i++;
        }

        String[] orderedVariableNames = variableStore.getOrderedNames();
        BigcuddLibrary.Cudd_DumpDot(dd,
                                    functions.length,
                                    Pointer.pointerToPointers(nodes),
                                    Pointer.pointerToCStrings(orderedVariableNames),
                                    Pointer.pointerToCStrings(functionNames),
                                    output);

        CUtils.fclose(output);
    }

    public void dumpDot(Map<String, ADD> functions, String fileName) {
        String[] functionNames = new String[functions.size()];
        ADD[] nodes = new ADD[functions.size()];

        // Do Map.values() and Map.keys() always return values and respective keys in the same order?
        // If so, we can avoid explicit iteration by using only these methods.
        int i = 0;
        for (Map.Entry<String, ADD> function: functions.entrySet()) {
            functionNames[i] = function.getKey();
            nodes[i] = function.getValue();
            i++;
        }
        dumpDot(functionNames, nodes, fileName);
    }

    public void dumpDot(String functionName, ADD function, String fileName) {
        dumpDot(new String[]{functionName},
                new ADD[]{function},
                fileName);
    }
    
    public void dumpDD(String functionName, ADD function, String fileName) {
    	Pointer<?> output = CUtils.fopen(fileName, CUtils.ACCESS_WRITE);

    	Pointer<Byte> ddname = Pointer.pointerToCString(functionName);
    	Pointer<DdNode> f = function.getUnderlyingNode();

    	String[] orderedVariableNames = variableStore.getOrderedNames();
    	Pointer<Pointer<Byte>> varnames = Pointer.pointerToCStrings(orderedVariableNames);
    	Pointer<Integer> auxids = null;

    	bigcudd.BigcuddLibrary.Dddmp_VarInfoType varinfo = bigcudd.BigcuddLibrary.Dddmp_VarInfoType.DDDMP_VARIDS;

    	Pointer<Byte> fname = Pointer.pointerToCString(fileName);
    	Pointer<?> fp = output;

    	bigcudd.BigcuddLibrary.Dddmp_cuddAddStore(dd,
    			ddname, 
    			f, 
    			varnames, 
    			auxids, 
    			bigcudd.BigcuddLibrary.DDDMP_MODE_TEXT,
    			varinfo, 
    			fname, 
    			fp);

    	CUtils.fclose(output);
    }
    
    /**
     * Writes an ADD to a text file using the functionality provided by
     * the dddmp library.
     * @param functionName Name of the function represented by the ADD (Can be empty or null).
     * @param add ADD to be stored to the file.
     * @param fileName Name of the text file to be written.
     */
    public void dumpADD(String functionName, ADD add, String fileName) {
        Pointer<?> output = CUtils.fopen(fileName, CUtils.ACCESS_WRITE);
        
        Pointer<Byte> ddname;
        if (functionName == null || functionName.isEmpty()) {
            ddname = null;
        } else {
            ddname = Pointer.pointerToCString(functionName);
        }

        String[] orderedVariableNames = variableStore.getOrderedNames(); 
        BigcuddLibrary.Dddmp_cuddAddStore(dd, 
                                          ddname, 
                                          add.getUnderlyingNode(), 
                                          Pointer.pointerToCStrings(orderedVariableNames), 
                                          null, 
                                          BigcuddLibrary.DDDMP_MODE_TEXT, 
                                          BigcuddLibrary.Dddmp_VarInfoType.DDDMP_VARIDS, 
                                          Pointer.pointerToCString(fileName), 
                                          output);
        CUtils.fclose(output);
    }

   
    public ADD readDD(String functionName) {

    	Dddmp_VarMatchType varMatchMode = Dddmp_VarMatchType.DDDMP_VAR_MATCHIDS;

    	String[] orderedVariableNames = variableStore.getOrderedNames();
    	Pointer<Pointer<Byte>> varMatchNames = Pointer.pointerToCStrings(orderedVariableNames);

    	Pointer<Integer> varmatchauxids = null;
    	Pointer<Integer> varcomposeids = null;

    	int mode = bigcudd.BigcuddLibrary.DDDMP_MODE_TEXT;
    	Pointer<Byte> fileName = Pointer.pointerToCString(functionName);
    	Pointer<?> file = CUtils.fopen(functionName, CUtils.ACCESS_READ);


    	Pointer<DdNode> answer = bigcudd.BigcuddLibrary.Dddmp_cuddAddLoad(dd,  
    			varMatchMode, 
    			varMatchNames, 
    			varmatchauxids, 
    			varcomposeids, 
    			mode, 
    			fileName, 
    			file);
    	if (answer == null) 
    		return null; 
    	else 
    		return new ADD(this.dd, answer, variableStore);
    	//		return 	answer;
    }

    public void dumpADD(ADD add, String fileName) {
        dumpADD(null, add, fileName);
    }
    
    /**
     * Reads an ADD from a text file written by the dumpADD method.
     * @param fileName Name of the file of containing the ADD.
     * @return New ADD instance containing the read information.
     */
    public ADD readADD(String fileName) {
        Pointer<?> input = CUtils.fopen(fileName, CUtils.ACCESS_READ);

        IntValuedEnum<BigcuddLibrary.Dddmp_VarMatchType> varMatchMode = BigcuddLibrary.Dddmp_VarMatchType.DDDMP_VAR_MATCHIDS;
        int mode = BigcuddLibrary.DDDMP_MODE_TEXT;
        Pointer<Byte> file = Pointer.pointerToCString(fileName);
        Pointer<DdNode> node = BigcuddLibrary.Dddmp_cuddAddLoad(dd,
                                                                varMatchMode,
                                                                null,
                                                                null,
                                                                null,
                                                                mode,
                                                                file,
                                                                input);

        CUtils.fclose(input);
        
        return new ADD(dd, node, variableStore);
    }
}
