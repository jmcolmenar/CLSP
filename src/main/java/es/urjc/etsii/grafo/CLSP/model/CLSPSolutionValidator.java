package es.urjc.etsii.grafo.CLSP.model;

import es.urjc.etsii.grafo.services.SolutionValidator;
import es.urjc.etsii.grafo.services.ValidationResult;

/**
 * Validate that a solution is valid for the CLSP problem.
 * Validation is always run after the algorithms executes, and can be run in certain algorithm stages to verify
 * that the current solution is valid.
 */
public class CLSPSolutionValidator extends SolutionValidator<CLSPSolution, CLSPInstance> {

    /**
     * Validate the current solution, check that no constraint is broken and everything is fine
     *
     * @param solution Solution to validate
     * @return ValidationResult.ok() if the solution is valid, ValidationResult.fail("reason why it failed") if a solution is not valid.
     */
    @Override
    public ValidationResult validate(CLSPSolution solution) {

        if (solution.getScore() < solution.getChangeoverTime()) {
            return ValidationResult.fail("Solution score is lower than changeover time");
        }

        for (int machine = 0; machine < solution.getInstance().getNumMachines(); machine++) {
            for (int w = 0; w < solution.getNumberOfMachineWorkSlots()[machine]; w++) {
                if (solution.getInstance().productionRate[solution.getSolutionData()[machine][w].getPartId()][machine] == 0) {
                    return ValidationResult.fail("Solution contains invalid workslot association: " + w + " is not producible in machine " + machine);
                }
            }
        }

        return ValidationResult.ok();
    }
}
