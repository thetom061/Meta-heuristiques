package jobshop.solvers;

import jobshop.Instance;
import jobshop.Result;
import jobshop.encodings.ResourceOrder;
import jobshop.encodings.Schedule;
import jobshop.encodings.Task;

import java.lang.reflect.Array;
import java.util.*;

/** An empty shell to implement a greedy solver. */
public class GreedySolver implements Solver {

    /** All possible priorities for the greedy solver. */
    public enum Priority {
        SPT, LPT, SRPT, LRPT, EST_SPT, EST_LPT, EST_SRPT, EST_LRPT
    }

    /** Priority that the solver should use. */
    final Priority priority;

    /** Creates a new greedy solver that will use the given priority. */
    public GreedySolver(Priority p) {
        this.priority = p;
    }

    @Override
    public Result solve(Instance instance, long deadline) {
        ResourceOrder greedyOrder = new ResourceOrder(instance);
        int numJobs= instance.numJobs;
        int numMachines=instance.numMachines-1;

        Set<Task> taches = new HashSet<>();
        //initialisation des tâches réalisables
        //liste donnant nombre de taches réalisés pour chaque job
        Integer[] tasksdone=new Integer[numJobs];
        //initialisation du tableau
        for (int l=0;l<numJobs;l++){
            tasksdone[l]=0;
        }
        //on mets les taches réalisables
        for (int j=0;j<numJobs;j++) {
            for (int i = 0; i < numMachines; i++) {
                if (instance.task_with_machine(j,i)==tasksdone[j]){
                    taches.add(new Task(j,tasksdone[j]));
                }
            }
        }
        while (!taches.isEmpty()) {
            Task addTask = null;
            int i=0;
            for(Task t : taches){
                if (i==0){
                    addTask=t;
                    i=1;
                }
                if (priority == Priority.SPT){
                    //on choppe le temps que ça met puis on compare et on update selon
                   if (instance.duration(addTask)>instance.duration(t)) {
                       addTask =t;
                    }
                } else if (priority == Priority.LPT) {
                    //on choppe le temps que ça met puis on compare et on update selon
                    if (instance.duration(addTask)<instance.duration(t)) {
                        addTask = t;
                    }
                }

                //faut rajouter dans l'ordre des machines
                greedyOrder.addTaskToMachine(instance.machine(addTask),addTask);

            }
            //on enlève la tâche ajouté dans greedyorder et on ajoute la suivante
            taches.remove(addTask);
            if (numMachines>=addTask.task+1) {
                taches.add(new Task(addTask.job, addTask.task + 1));
            }
        }
        
        Result result= new Result(instance,greedyOrder.toSchedule(),Result.ExitCause.ProvedOptimal);

        return result;
    }
}
