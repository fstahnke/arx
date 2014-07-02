/*
 * ARX: Efficient, Stable and Optimal Data Anonymization
 * Copyright (C) 2012 - 2014 Florian Kohlmayer, Fabian Prasser
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package org.deidentifier.arx.algorithm;

import org.deidentifier.arx.framework.check.INodeChecker;
import org.deidentifier.arx.framework.check.history.History.PruningStrategy;
import org.deidentifier.arx.framework.check.history.History.StorageStrategy;
import org.deidentifier.arx.framework.lattice.Lattice;
import org.deidentifier.arx.framework.lattice.Node;

/**
 * This class provides a reference implementation of the Linear FLASH Algorithm.
 * 
 * @author Fabian Prasser
 * @author Florian Kohlmayer
 */
public class FLASHAlgorithmLinear extends AbstractFLASHAlgorithm {

    /**
     * Creates a new instance of the FLASH algorithm.
     * 
     * @param lattice
     *            The lattice
     * @param history
     *            The history
     * @param checker
     *            The checker
     * @param strategy
     *            The strategy
     */
    public FLASHAlgorithmLinear(final Lattice lattice,
                          final INodeChecker checker,
                          final FLASHStrategy strategy) {

        super(lattice, checker, strategy);
        history.setPruningStrategy(PruningStrategy.CHECKED);
        history.setStorageStrategy(StorageStrategy.ALL);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.deidentifier.ARX.algorithm.AbstractAlgorithm#traverse()
     */
    @Override
    public void traverse() {
        
        // Init
        pqueue.clear();
        stack.clear();
        if (!lattice.getBottom().isChecked()) checker.check(lattice.getBottom(), true);
        
        // For each node
        final int length = lattice.getLevels().length;
        for (int i = 0; i < length; i++) {
            Node[] level;
            level = this.sort(i);
            for (final Node node : level) {
                if (!node.isTagged()) { 
                    pqueue.add(node);
                    while (!pqueue.isEmpty()) {
                        Node head = pqueue.poll();
                        
                        // If anonymity is unknown
                        if (!head.isTagged()) {
                        	
                            // Change strategies
                            final PruningStrategy pruningStrategy = history.getPruningStrategy();
                            final StorageStrategy storageStrategy = history.getStorageStrategy();
                            history.setPruningStrategy(PruningStrategy.CHECKED);
                            history.setStorageStrategy(StorageStrategy.ALL);

                            // Second phase
                            stack.push(head);
                            while (!stack.isEmpty()) {
                                final Node start = stack.pop();
                                if (!start.isTagged()) {
                                    findPath(start);
                                    checkPathLinear(path);
                                }
                            }

                            // Switch back to previous strategies
                            history.setPruningStrategy(pruningStrategy);
                            history.setStorageStrategy(storageStrategy);
                        }
                    }
                }
            }
        }
        
        if (lattice.getTop().getInformationLoss() == null) {
            if (!lattice.getTop().isChecked())  checker.check(lattice.getTop(), true);
        }
    }
}
