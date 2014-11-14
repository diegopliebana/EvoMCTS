import pylab
import numpy as np
import matplotlib.pyplot as plt
import operator
from matplotlib.pyplot import errorbar

def errorfill(x, y, yerr, color=None, alpha_fill=0.3, ax=None):
    ax = ax if ax is not None else plt.gca()
    if color is None:
        color = ax._get_lines.color_cycle.next()
    if np.isscalar(yerr) or len(yerr) == len(y):
        ymin = [a - b for a,b in zip(y, yerr)] 
        ymax = [sum(pair) for pair in zip(y, yerr)] 
    elif len(yerr) == 2:
        ymin, ymax = yerr

    ax.plot(x, y, color=color)
    print ymin
    print y
    print ymax
    ax.fill_between(x, ymax, ymin, color=color, alpha=alpha_fill)
    
#Take the data
#datafile = 'inout/sampleH3.csv'

#filenames = ['SampleMCTS', 'FEMCTS-HT', 'FEMCTS-RND', 'FEMCTS-11', 'FEMCTS-U1']
#filenames = ['SampleMCTS', 'SampleMCTS-NODF']
#filenames = ['FEMCTS-HT', 'FEMCTS-HT-NODF']

#filenames = ['TEVC_MCTS_100iter_K10_100r','TEVC_MCTS_500iter_K2_100r','TEVC_MCTS_1000iter_K1_100r','sampleMCTS_1000iter_100r']
#filenames = ['sampleMCTS_100iter_100r_decay0.5','TEVC_MCTS_100iter_K1_100r_decay0.5']
#filenames = ['sampleMCTS_1000iter_100r_decay0.5','TEVC_MCTS_100iter_K10_100r_decay0.5']
#filenames = ['FEMCTS-BanditAllRandom_Dist10','FEMCTS-BanditAllRandom_Dist9','FEMCTS-BanditAllRandom_Dist8',
#             'FEMCTS-BanditAllRandom_Dist7', 'FEMCTS-BanditAllRandom_Dist6','FEMCTS-BanditAllRandom_Dist5']

filenames = ['sampleMCTS_controllers.sampleMCTS.Agent_Dist14','sampleMCTS_controllers.sampleMCTS.Agent_Dist13','sampleMCTS_controllers.sampleMCTS.Agent_Dist12',
             'sampleMCTS_controllers.sampleMCTS.Agent_Dist11','sampleMCTS_controllers.sampleMCTS.Agent_Dist10','sampleMCTS_controllers.sampleMCTS.Agent_Dist19',
             'sampleMCTS_controllers.sampleMCTS.Agent_Dist8','sampleMCTS_controllers.sampleMCTS.Agent_Dist7','sampleMCTS_controllers.sampleMCTS.Agent_Dist6',
             'sampleMCTS_controllers.sampleMCTS.Agent_Dist5','sampleMCTS_controllers.sampleMCTS.Agent_Dist4','sampleMCTS_controllers.sampleMCTS.Agent_Dist3',
             'sampleMCTS_controllers.sampleMCTS.Agent_Dist2','sampleMCTS_controllers.sampleMCTS.Agent_Dist1']


#Create a figure
fig = pylab.figure()

#Add a subplot (Grid of plots 1x1, adding plot 1)
ax = fig.add_subplot(111)  

for filename in filenames:

    #datafile = '../circle/' + filename + '.csv'
    #datafile = '../leftright/fixedPos/' + filename + '.txt'
    datafile = '../circle/fixedPos/' + filename + '.txt'
    #datafile = '../tomConsoleRuns/' + filename + '.txt'

    print 'loading', datafile
    r = pylab.loadtxt(datafile, comments='#', delimiter=',')
    
    NumberOfColumns = len(r[0])
    averages = [0] * NumberOfColumns 
    std_devs = [0] * NumberOfColumns
    std_errs = [0] * NumberOfColumns
    roll_depth  = [0] * NumberOfColumns
    
    for i in range(NumberOfColumns):
        roll_depth[i] = i+1
        averages[i] = np.average(r[:,i])
        std_devs[i] = np.std(r[:,i])
        std_errs[i] = np.std(r[:,i]) / np.sqrt(len(r[:,i]))
    


#Plot everything in this subplot.
#ax.plot(roll_depth,averages,'-',linewidth=2.0)
#ax.plot(gen,best,'--',linewidth=2.0)
#ax.plot(iterations,average,'-',linewidth=2.0)

#errorbar(iterations,average)
#errorbar(iterations,average,yerr=stderr)

# plot all points without error bars
#ax.plot(iterations, fitness)
# plot errorbars for every 50th point
#errorbar(roll_depth,averages,std_errs, linestyle='None')

        
    errorfill(roll_depth,averages,std_errs)
    #errorfill(roll_depth[-7:],averages[-7:],std_errs[-7:]) #for leftright
    #errorfill(roll_depth[-5:],averages[-5:],std_errs[-5:]) #for circle


##Add the legend
#plt.legend(('1+1','HT'), 
#           shadow=True, fancybox=True)

plt.legend(filenames, 
           shadow=True, fancybox=True) 

#Titles and labels
#plt.title('Heuristic estimation: 8 routes')
plt.xlabel("Rollout length", fontsize=16)#
plt.ylabel("Average time steps", fontsize=16)

plt.ylim([0,2])


#Save to file.
fig.savefig("../leftright/fixedPos/SampleMCTS_vs_FEMCTS-BanditAllRandom_Dist10-6.pdf")

# And show it:
plt.show()
