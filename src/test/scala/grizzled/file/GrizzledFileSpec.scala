package grizzled.file

import grizzled.BaseSpec
import Implicits.GrizzledFile
import grizzled.file.util.withTemporaryDirectory
import grizzled.file.{util => fileutil}

import java.io.File


class GrizzledFileSpec extends BaseSpec {

  "touch" should "create a file if it does not exist" in {
    withTemporaryDirectory("GrizzledFile") { dir =>
      val path = fileutil.joinPath(dir, new File("foobar.txt"))
      path should not (exist)
      path.touch() should be (success)
      path should (exist)
    }
  }
  "pathExists" should "return Success for an existing file" in {
    withTemporaryDirectory("GrizzedFile") { dir =>

    }
  }
}
