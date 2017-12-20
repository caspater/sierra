/*
    
    Copyright (C) 2017 Stanford HIVDB team
    
    Sierra is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.
    
    Sierra is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.
    
    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package edu.stanford.hivdb.ngs;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import edu.stanford.hivdb.mutations.CodonTranslation;
import edu.stanford.hivdb.mutations.Gene;
import edu.stanford.hivdb.mutations.MutationSet;

public class GeneSequenceReads {
	
	private final Gene gene;
	private final int firstAA;
	private final int lastAA;
	private final List<PositionCodonReads> posCodonReads;
	private final double minPrevalence;
	private MutationSet mutations;
	
	public GeneSequenceReads(
			final Gene gene,
			final List<PositionCodonReads> posCodonReads,
			final double minPrevalence) {
		this.gene = gene;
		this.posCodonReads = Collections.unmodifiableList(posCodonReads);
		this.minPrevalence = minPrevalence;
		this.firstAA = (int) posCodonReads.get(0).getPosition();
		this.lastAA = (int) posCodonReads.get(posCodonReads.size() - 1).getPosition();
	}
	
	/** initializes GeneSequence without specify gene
	 * 
	 * Warning: This constructor is only intended to use internally
	 * 
	 * @param posCodonReads
	 * @param minPrevalence
	 */
	protected GeneSequenceReads(
			final List<PositionCodonReads> posCodonReads,
			final double minPrevalence) {
		this(posCodonReads.get(0).getGene(), posCodonReads, minPrevalence);
	}
	
	public Gene getGene() { return gene; }
	public int getFirstAA() { return firstAA; }
	public int getLastAA() { return lastAA; }
	public int getSize() { return lastAA - firstAA + 1; }
	
	public MutationSet getMutations(final double minPrevalence) {
		MutationSet retMuts;
		if (minPrevalence == this.minPrevalence && mutations != null) {
			retMuts = mutations;
		} else {
			retMuts = posCodonReads
				.stream()
				.map(pcr -> pcr.getMutations(minPrevalence))
				.reduce(new MutationSet(), (ms1, ms2) -> ms1.mergesWith(ms2));
			if (minPrevalence == this.minPrevalence) {
				mutations = retMuts;
			}
		}
		return retMuts;
	}
	
	public MutationSet getMutations() {
		return getMutations(this.minPrevalence);
	}
	
	/** Returns consensus sequence aligned to subtype B reference.
	 *  All insertions are removed from the result.
	 * 
	 * @param autoComplete specify <tt>true</tt> to prepend and/or append
	 * wildcard "." to incomplete sequence
	 * @return the aligned consensus sequence
	 */
	public String getAlignedNAs(boolean autoComplete) {
		StringBuilder seq = new StringBuilder();
		if (autoComplete) {
			seq.append(StringUtils.repeat("...", firstAA - 1));
		}
		seq.append(posCodonReads
			.stream()
			.map(pcr -> pcr.getCodonConsensus(minPrevalence))
			.collect(Collectors.joining()));
		if (autoComplete) {
			seq.append(StringUtils.repeat("...", gene.getLength() - lastAA));
		}
		return seq.toString();
	}

	/** Returns consensus sequence aligned to subtype B reference without
	 *  initial and trailing "." for incomplete sequence. All insertions are
	 *  removed from the result. The result is equivalent to the result of
	 *  <tt>getAlignedSequence(false)</tt>.
	 * 
	 * @return the aligned consensus NA sequence
	 */ 
	public String getAlignedNAs() {
		return getAlignedNAs(false);
	}

	/** Returns consensus sequence aligned to subtype B reference in amino
	 *  acid form. Unsequenced region(s) are ignored. All insertions are
	 *  removed from the result.
	 *
	 * @return the aligned consensus AA sequence
	 */
	public String getAlignedAAs() {
		return CodonTranslation.simpleTranslate(
			this.getAlignedNAs(false), firstAA, gene.getConsensus());
	}

}