package org.broadinstitute.hail.driver

import org.broadinstitute.hail.methods.SamplePCA
import org.kohsuke.args4j.{Option => Args4jOption}
import org.broadinstitute.hail.Utils._

object PCA extends Command {
  def name = "pca"

  def description = "Compute PCA on the matrix of genotypes"

  class Options extends BaseOptions {
    @Args4jOption(required = true, name = "-o", aliases = Array("--output"), usage = "Output file")
    var output: String = _

    @Args4jOption(required = false, name = "-k", aliases = Array("--components"), usage = "Number of principal components")
    var k: Int = 10

    @Args4jOption(required = false, name = "-l", aliases = Array("--loadings"), usage = "Compute loadings")
    var l: Boolean = _

    @Args4jOption(required = false, name = "-e", aliases = Array("--eigenvalues"), usage = "Compute eigenvalues")
    var e: Boolean = _

  }

  def newOptions = new Options

  def run(state: State, options: Options): State = {

    val vds = state.vds
    val filename = options.output.replaceAll(".tsv", "")

    val (scores, loadings, eigenvalues) = (new SamplePCA(options.k, options.l, options.e))(vds)

    writeTextFile(filename + ".tsv", state.hadoopConf) { s =>
      s.write("sample")
      for (i <- 0 until options.k)
        s.write("\t" + "PC" + (i + 1))
      s.write("\n")
      for (i <- 0 until vds.nLocalSamples) {
        s.write(vds.sampleIds(vds.localSamples(i)))
        for (j <- 0 until options.k)
          s.write("\t" + scores(i, j))
        s.write("\n")
      }
    }

    if (options.l) {
      val vls = loadings.collect() //FIXME: Sort!
      writeTextFile(filename + ".loadings.tsv", state.hadoopConf) { s =>
        s.write("chrom\t pos\t ref\t alt")
        for (i <- 0 until options.k)
          s.write("\t" + "PC" + (i + 1))
        s.write("\n")
        for (i <- 0 until vds.nVariants.toInt) {
          val (v, l) = vls(i)
          s.write(v.contig + "\t" + v.start + "\t" + v.ref + "\t" + v.alt)
          for (j <- 0 until options.k)
            s.write("\t" + l(j))
          s.write("\n")
        }
      }
    }

    if (options.e)
      writeTextFile(filename + ".eigen.tsv", state.hadoopConf) { s =>
        for (i <- 0 until options.k)
          s.write(eigenvalues(i) + "\n")
      }

    state
  }
}
