package sriracha.simulator.model.elements.ctlsources;

import sriracha.simulator.solver.analysis.ac.ACEquation;
import sriracha.simulator.solver.analysis.dc.DCEquation;

public class VCVS extends ControlledSource {


    private int currentIndex;


    /**
     * Voltage controlled Voltage Source
     * Vcvs: Vs =  gm * v0
     * where v0 is a voltage elsewhere in the circuit
     * Vs is the source voltage
     *
     * @param name - VCVS name from netlist
     * @param gm   - factor in source Voltage equation
     */
    public VCVS(String name, double gm) {
        super(name, gm);
    }


    @Override
    public void applyAC(ACEquation equation) {
        equation.applyRealMatrixStamp(ncMinus, currentIndex, gm);
        equation.applyRealMatrixStamp(ncPlus, currentIndex, -gm);
        equation.applyRealMatrixStamp(nPlus, currentIndex, 1);
        equation.applyRealMatrixStamp(nMinus, currentIndex, -1);
        equation.applyRealMatrixStamp(currentIndex, nPlus, 1);
        equation.applyRealMatrixStamp(currentIndex, nMinus, -1);
    }

    @Override
    public void applyDC(DCEquation equation) {
        equation.applyMatrixStamp(ncMinus, currentIndex, gm);
        equation.applyMatrixStamp(ncPlus, currentIndex, -gm);
        equation.applyMatrixStamp(nPlus, currentIndex, 1);
        equation.applyMatrixStamp(nMinus, currentIndex, -1);
        equation.applyMatrixStamp(currentIndex, nPlus, 1);
        equation.applyMatrixStamp(currentIndex, nMinus, -1);
    }

    @Override
    public int getNodeCount() {
        return 2;
    }

    @Override
    public int getExtraVariableCount() {
        return 1;
    }

    /**
     * This is used to build a copy of the circuit element during netlist parsing
     * when adding multiple elements with the same properties.
     * Node information will of course not be copied and have to be entered afterwards
     */
    @Override
    public VCVS buildCopy(String name) {
        return new VCVS(name, gm);
    }


    @Override
    public void setFirstVarIndex(int i) {
        currentIndex = i;
    }
}
