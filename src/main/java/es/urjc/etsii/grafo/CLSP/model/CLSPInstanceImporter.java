package es.urjc.etsii.grafo.CLSP.model;

import es.urjc.etsii.grafo.io.InstanceImporter;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Scanner;

public class CLSPInstanceImporter extends InstanceImporter<CLSPInstance> {

    @Override
    public CLSPInstance importInstance(BufferedReader reader, String filename) throws IOException {
        // Create and return instance object from file data

        // Skip all the comments of the file, which must exist only at the beginning
        while (reader.readLine().startsWith("#"));

        Scanner sc = new Scanner(reader);
        var numParts = sc.nextInt();
        var numMachines = sc.nextInt();
        var numPeriods = sc.nextInt();

        var productionRate = new int[numParts][numMachines];
        for (int i = 0; i < numParts; i++) {
            for (int j = 0; j < numMachines; j++) {
                productionRate[i][j] = sc.nextInt();
            }
        }

        var changeoverTime = new int[numParts][numParts];
        for (int i = 0; i < numParts; i++) {
            for (int j = 0; j < numParts; j++) {
                changeoverTime[i][j] = sc.nextInt();
            }
        }

        var inventory = new int[numParts][numPeriods];
        for (int i = 0; i < numParts; i++) {
            for (int j = 0; j < numPeriods; j++) {
                inventory[i][j] = sc.nextInt();
            }
        }

        var capacity = new int[numMachines][numPeriods];
        for (int i = 0; i < numMachines; i++) {
            for (int j = 0; j < numPeriods; j++) {
                capacity[i][j] = sc.nextInt();
            }
        }

        var priority = new int[numParts][numMachines];
        for (int i = 0; i < numParts; i++) {
            for (int j = 0; j < numMachines; j++) {
                priority[i][j] = sc.nextInt();
            }
        }

        // Call instance constructor when we have parsed all the data

        // IMPORTANT! Remember that instance data must be immutable from this point
        return new CLSPInstance(filename,numParts,numMachines,numPeriods,productionRate,changeoverTime,inventory,capacity,priority);
    }

    // Create main function to test the class methods
    public static CLSPInstance checkInstance(String filename)  {
        CLSPInstance instance = null;

        // Create instance importer object
        var importer = new CLSPInstanceImporter();
        try {
            // Create buffered reader object
            BufferedReader reader = new BufferedReader(new java.io.FileReader(filename));
            // Import instance
            instance = importer.importInstance(reader, filename);

        } catch (FileNotFoundException e) {
            System.out.println("File not found");
        } catch (IOException e) {
            System.out.println("Error reading file");
        }

        // Print instance
        System.out.println(instance);

        return instance;
    }

//    public static void main(String[] args) {
//        checkInstance( "instances/testing/toy-instance.txt");
//    }
}
