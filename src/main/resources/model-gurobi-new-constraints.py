from gurobipy import Model, GRB, quicksum, tuplelist
import sys


def pause():
    print("Press the <ENTER> key to continue...")
    input()


def schedule(instance,time_limit):

    ###############################################################
    # FROM INSTANCE TO MODEL VARIABLES ############################
    ###############################################################
    # General data
    parts = []
    for part in range(instance["num_parts"]):
        parts.append(part)
    periods = []
    for period in range(instance["num_periods"]):
        periods.append(1.0*(period+1))
    lines = []
    for line in range(instance["num_machines"]):
        lines.append(line)
    changeover_time = instance["changeover_time"]
    # Parts per hour and pairs of (part,line)
    parts_per_hour = {}
    pairs = tuplelist()
    parts_in_line = {}
    priority = {}
    for part in parts:
        for line in lines:
            if instance["production_rates"][part, line] > 0:
                parts_per_hour[part, line] = instance["production_rates"][part, line]
                pairs.append((part, line))
                if line not in parts_in_line.keys():
                    parts_in_line[line] = []
                parts_in_line[line].append(part)
                if instance["priority"][part, line] > 1:
                    priority[part, line] = 1
                else:
                    priority[part, line] = 0
    # Assume same workweek for all lines
    workweek = instance["capacity"][0, 0]
    # Adapt coverage to model
    coverage = {}
    for period in periods:
        coverage[period] = {}
        for part in parts:
            coverage[period][part] = instance["inventory"][part, period-1]
    # Long changeover time
    long_changeover = max(changeover_time.values())


    ###############################################################
    # MODEL DEFINITION  ###########################################
    ###############################################################
    # lotsizing and scheduling model
    model = Model('mip_model')
    production_hours = model.addVars(pairs, periods, name="production_hours")
    position = model.addVars(pairs, periods, name="position")
    shortage = model.addVars(parts, periods, name="shortage")
    initial_setup = model.addVars(pairs, vtype=GRB.BINARY, name="initial_setup")
    carried_over = model.addVars(pairs, periods, vtype=GRB.BINARY, name="carried_over")
    part_change = {}
    for line in lines:
        for from_part in parts_in_line[line]:
            for to_part in parts_in_line[line]:
                if from_part != to_part:
                    for period in periods:
                        part_change[from_part, to_part, line, period] = model.addVar(vtype=GRB.BINARY,
                                                                                     name='part_change[' + str(
                                                                                         from_part) + ',' + str(
                                                                                         to_part) + ',' + str(
                                                                                         line) + ',' + str(
                                                                                         period) + ']')

    # demand coverage constraints (4)
    for part in parts:
        for period in periods:
            model.addConstr(quicksum(
                parts_per_hour[part, line] * production_hours[part, line, periods[p]] for line in lines if
                (part, line) in pairs for p in range(periods.index(period) + 1)) + coverage[period][part] + shortage[
                                part, period] >= 0)

    # capacity constraints (5)
    model.addConstrs(production_hours.sum('*', line, period) + quicksum(
        changeover_time[from_part, to_part] * part_change[from_part, to_part, line, period] for from_part in
        parts_in_line[line] for to_part in parts_in_line[line] if from_part != to_part) <= workweek for line in lines
                     for period in periods)

    # linking constraints for first period (6) and (7)
    model.addConstrs(production_hours[part, line, periods[0]] <= workweek * (quicksum(
        part_change[from_part, part, line, periods[0]] for from_part in parts_in_line[line] if from_part != part) +
                                                                             initial_setup[part, line]) for (part, line)
                     in pairs)
    model.addConstrs(production_hours[part, line, periods[0]] >= long_changeover * (quicksum(
        part_change[from_part, part, line, periods[0]] for from_part in parts_in_line[line] if from_part != part) +
                                                                                    initial_setup[part, line]) for
                     (part, line) in pairs)

    # linking constraints for periods after first (6) and (7)
    model.addConstrs(production_hours[part, line, periods[p]] <= workweek * (quicksum(
        part_change[from_part, part, line, periods[p]] for from_part in parts_in_line[line] if from_part != part) +
                                                                             carried_over[part, line, periods[p - 1]])
                     for (part, line) in pairs for p in range(1, len(periods)))
    model.addConstrs(production_hours[part, line, periods[p]] >= long_changeover * (quicksum(
        part_change[from_part, part, line, periods[p]] for from_part in parts_in_line[line] if from_part != part) +
                                                                                    carried_over[
                                                                                        part, line, periods[p - 1]]) for
                     (part, line) in pairs for p in range(1, len(periods)))

    # changeover flow constraints for first period (8) and (9)
    model.addConstrs(initial_setup[part, line] + quicksum(
        part_change[from_part, part, line, periods[0]] for from_part in parts_in_line[line] if from_part != part) ==
                     carried_over[part, line, periods[0]] + quicksum(
        part_change[part, to_part, line, periods[0]] for to_part in parts_in_line[line] if to_part != part) for
                     (part, line) in pairs)
    model.addConstrs(initial_setup.sum('*', line) == 1 for line in lines)

    # changeover flow constraints for periods after first (8) and (9)
    model.addConstrs(carried_over[part, line, periods[p - 1]] + quicksum(
        part_change[from_part, part, line, periods[p]] for from_part in parts_in_line[line] if from_part != part) ==
                     carried_over[part, line, periods[p]] + quicksum(
        part_change[part, to_part, line, periods[p]] for to_part in parts_in_line[line] if to_part != part) for
                     (part, line) in pairs for p in range(1, len(periods)))
    model.addConstrs(carried_over.sum('*', line, period) == 1 for line in lines for period in periods)

    # position constraints (10)
    for line in lines:
        for from_part in parts_in_line[line]:
            for to_part in parts_in_line[line]:
                if from_part != to_part:
                    for period in periods:
                        model.addConstr(
                            position[to_part, line, period] >= position[from_part, line, period] + 1 - len(parts) * (
                                    1 - part_change[from_part, to_part, line, period]))

    # objective function
    total_shortage = shortage.sum('*', '*')
    total_changeover_time = quicksum(
        changeover_time[from_part, to_part] * part_change[from_part, to_part, line, period] for line in lines for
        from_part in parts_in_line[line] for to_part in parts_in_line[line] if from_part != to_part for period in
        periods)
    total_nonpriority_hours = quicksum(
        priority[part, line] * production_hours[part, line, period] for (part, line) in pairs for period in periods)
    model.setObjective(total_shortage+total_changeover_time)
    # model.setObjectiveN(total_shortage, index=0, priority=2)
    # model.setObjectiveN(total_changeover_time, index=1, priority=1)
    # model.setObjectiveN(total_nonpriority_hours, index=2, priority=0)

    # model.write("model.lp")
    model.Params.TimeLimit = time_limit
    # model.Params.MipGap = 0.07
    # model.Params.MipFocus = 3
    # model.Params.ImproveStartTime = 10
    # model.Params.OutputFlag = 1

    print("\n>>> Solving model...\n")
    model.optimize()
    if model.status == GRB.INFEASIBLE:
        print("\n>>> No feasible solution found.")
        model.computeIIS()
        model.write("model.ilp")
        sys.exit()
    else:
        print("\n>>> Solution found:")
        print("Shortage:          {0:6.0f}".format(total_shortage.getValue()))
        print("Changeover time:   {0:6.0f}".format(total_changeover_time.getValue()))
        print("Priority:          {0:6.1f}".format(total_nonpriority_hours.getValue()))
        print("\n>>> Model variables:")
        model.printAttr('x')

        ###############################################################
        # PRINT SOLUTION SCHEDULE  ####################################
        ###############################################################
        print("\n\n>>> Solution Schedule:")
        print("Line\tPart\tPriority\tHours\tProduced_Parts\tPeriod")
        for line in lines:
            from_part = 'Null'
            period_index = 0
            while period_index < len(periods):
                if from_part == 'Null':
                    if period_index == 0:
                        for to_part in parts_in_line[line]:
                            if initial_setup[to_part, line].x > 0.5:
                                part = to_part
                                break
                else:
                    for to_part in parts_in_line[line]:
                        if from_part != to_part:
                            if part_change[from_part, to_part, line, periods[period_index]].x > 0.5:
                                part = to_part
                                break
                print(line,
                      part,
                      priority[part, line],
                      production_hours[part, line, periods[period_index]].x,
                      parts_per_hour[part, line] * production_hours[part, line, periods[period_index]].x,
                      periods[period_index],
                      sep='\t',end='\n')
                if carried_over[part, line, periods[period_index]].x > 0.5:
                    period_index += 1
                    from_part = 'Null'
                else:
                    from_part = part


def read_matrix(f, rows, columns):
    matrix = {}
    for r in range(rows):
        line = f.readline().split()
        for c in range(columns):
            matrix[r, c] = float(line[c])
    return matrix


def load_instance(file_name):
    instance = {}
    with open(file_name, "r") as f:
        # Read header
        l = f.readline()
        while l.startswith("#") or l.startswith("\n"):
            l = f.readline()

        # Read main data
        instance["num_parts"] = int(l)
        instance["num_machines"] = int(f.readline())
        instance["num_periods"] = int(f.readline())

        # Read production rates
        instance["production_rates"] = read_matrix(f, instance["num_parts"], instance["num_machines"])
        # Read changeover times
        instance["changeover_time"] = read_matrix(f, instance["num_parts"], instance["num_parts"])
        # Read inventory
        instance["inventory"] = read_matrix(f, instance["num_parts"], instance["num_periods"])
        # Read capacity
        instance["capacity"] = read_matrix(f, instance["num_machines"], instance["num_periods"])
        # Read priority
        instance["priority"] = read_matrix(f, instance["num_parts"], instance["num_machines"])

    f.close()

    print("\n>>> Instance " + file_name + " successfully loaded.\n")

    return instance


if __name__ == "__main__":
    default_instance = "toy-instance.txt"
    if len(sys.argv) < 2:
        print("\n>>> No instance file provided. Using default instance: " + default_instance+"\n")
        instance = load_instance(default_instance)
    else:
        instance = load_instance(sys.argv[1])

    time_limit = 5
    if len(sys.argv) < 3:
        print("\n>>> No time limit provided. Using default time limit: " + str(time_limit) + " seconds.\n")
    else:
        time_limit = int(sys.argv[2])

    schedule(instance, time_limit)
