package org.broadinstitute.hail.methods

import org.broadinstitute.hail.SparkSuite
import org.testng.annotations.Test

class PedigreeSuite extends SparkSuite {
  @Test def test() {
    val vds = LoadVCF(sc, "src/test/resources/sample_mendel.vcf")
    val ped = Pedigree.read("src/test/resources/sample_mendel.fam", sc.hadoopConfiguration, vds.sampleIds)
    ped.write("/tmp/sample_mendel.fam", sc.hadoopConfiguration, vds.sampleIds)  // FIXME: this is not right
    val pedwr = Pedigree.read("/tmp/sample_mendel.fam", sc.hadoopConfiguration, vds.sampleIds)
    assert(ped.trios.sameElements(pedwr.trios)) // this passes because all samples in .fam are in sample_mendel.vcf

    val nuclearFams = Pedigree.nuclearFams(ped.completeTrios)
    val sampleIndex = vds.sampleIds.zipWithIndex.toMap
    assert(nuclearFams((sampleIndex("Dad1"), sampleIndex("Mom1"))).toSet ==
      Set(sampleIndex("Son1"), sampleIndex("Dtr1")))
    assert(nuclearFams((sampleIndex("Dad2"), sampleIndex("Mom2"))).toSet ==
      Set(sampleIndex("Son2")))
    assert(nuclearFams.size == 2 && ped.completeTrios.length == 3 && ped.trios.length == 11)

    assert(ped.nSatisfying(_.isMale) == 6 && ped.nSatisfying(_.isFemale) == 5)

    assert(ped.nSatisfying(_.isCase) == 4 && ped.nSatisfying(_.isControl) == 3)

    assert(ped.nSatisfying(_.isComplete, _.isMale) == 2 && ped.nSatisfying(_.isComplete, _.isFemale) == 1 &&
      ped.nSatisfying(_.isComplete, _.isCase) == 2 && ped.nSatisfying(_.isComplete, _.isControl) == 1)

    assert(ped.nSatisfying(_.isComplete, _.isCase, _.isMale) == 1 &&
      ped.nSatisfying(_.isComplete, _.isCase, _.isFemale) == 1 &&
      ped.nSatisfying(_.isComplete, _.isControl, _.isMale) == 1 &&
      ped.nSatisfying(_.isComplete, _.isControl, _.isFemale) == 0)
  }

  @Test def testWithMismatchedSamples() {
    val vds = LoadVCF(sc, "src/test/resources/sample_mendel.vcf")
    val ped = Pedigree.read("src/test/resources/pedigree.fam", sc.hadoopConfiguration, vds.sampleIds)

    val nuclearFams = Pedigree.nuclearFams(ped.completeTrios)
    val sampleIndex = vds.sampleIds.zipWithIndex.toMap
    assert(nuclearFams((sampleIndex("Dad1"), sampleIndex("Mom1"))).toSet ==
      Set(sampleIndex("Son1"), sampleIndex("Dtr1"))) // Baby1 is dropped since it's not in the vcf
    assert(nuclearFams((sampleIndex("Dad2"), sampleIndex("Mom2"))).toSet ==
      Set(sampleIndex("Son2")))
    assert(nuclearFams.size == 2 && ped.completeTrios.length == 3 && ped.trios.length == 7)

    assert(ped.nSatisfying(_.isMale) == 4 && ped.nSatisfying(_.isFemale) == 3)
  }

  //FIXME: How to test
  //ped.writeSummary("/tmp/sample_mendel.sumfam", sc.hadoopConfiguration)

}
