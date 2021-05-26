package jobshop.solvers;

import jobshop.Instance;
import jobshop.Result;
import jobshop.encodings.ResourceOrder;
import jobshop.encodings.Schedule;
import jobshop.solvers.neighborhood.Neighbor;
import jobshop.solvers.neighborhood.Neighborhood;
import jobshop.solvers.neighborhood.Nowicki;

import java.util.List;

/** An empty shell to implement a descent solver. */
public class DescentSolver implements Solver {

    final Neighborhood<ResourceOrder> neighborhood;
    final Solver baseSolver;
    private static int neighborsexplored=0;
    /** Creates a new descent solver with a given neighborhood and a solver for the initial solution.
     *
     * @param neighborhood Neighborhood object that should be used to generates neighbor solutions to the current candidate.
     * @param baseSolver A solver to provide the initial solution.
     */
    public DescentSolver(Neighborhood<ResourceOrder> neighborhood, Solver baseSolver) {
        this.neighborhood = neighborhood;
        this.baseSolver = baseSolver;
    }
    public int getNeighborsexplored(){
        return neighborsexplored;
    }

    @Override
    public Result solve(Instance instance, long deadline) {
        Schedule base=baseSolver.solve(instance,deadline).schedule.get();
        ResourceOrder order= new ResourceOrder(base);

        int bestspan=Integer.MAX_VALUE,currentspan,initialspan=base.makespan();
        int i=0;
        Neighbor<ResourceOrder> bestneigh=null,prevbestneigh=null;
        do {
            //on initialise les voisins
            List<Neighbor<ResourceOrder>> neigh=neighborhood.generateNeighbors(order);
            //on trouve le meilleur
            prevbestneigh=bestneigh;

            //on iter sur les voisins
            for (Neighbor<ResourceOrder> currentneighbor : neigh) {
                neighborsexplored++;
                currentneighbor.applyOn(order);
                //il faut que le chemin soit valable
                if (!order.toSchedule().isEmpty()) {
                    currentspan = order.toSchedule().get().makespan();
                    if (currentspan < bestspan) {
                        bestspan = currentspan;
                        bestneigh = currentneighbor;
                    }
                    currentneighbor.undoApplyOn(order);
                }
            }
            if (bestspan < initialspan) {
                bestneigh.applyOn(order);
            }
            i++;
        }while(prevbestneigh!=bestneigh & i<deadline);

        Result result= new Result(instance,order.toSchedule(),Result.ExitCause.ProvedOptimal);
        return result;
    }

}
