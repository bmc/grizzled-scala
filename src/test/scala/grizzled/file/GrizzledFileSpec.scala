package grizzled.file

import grizzled.BaseSpec
import Implicits.GrizzledFile
import grizzled.file.util.withTemporaryDirectory
import grizzled.file.{util => fileutil}
import grizzled.util.withResource
import grizzled.util.CanReleaseResource.Implicits.CanReleaseAutoCloseable
import java.io.{File, FileWriter}

class GrizzledFileSpec extends BaseSpec {

  "touch" should "create a file if it does not exist" in {
    withTemporaryDirectory("GrizzledFile") { dir =>
      val path = new File(fileutil.joinPath(dir.getAbsolutePath, "foobar.txt"))
      path should not (exist)
      path.touch() shouldBe 'success
      path should exist
    }
  }

  it should "update the timestamp of a file that exists" in {
    withTemporaryDirectory("GrizzledFile") { dir =>
      val path = new File(fileutil.joinPath(dir.getAbsolutePath, "foo.txt"))
      withResource(new FileWriter(path)) { w =>
        w.write("Some content\n")
      }
      path should exist
      val creationTime = path.lastModified
      val newTime = creationTime - 86400
      path.touch(newTime)

      // lastModified is only guaranteed to be within second granularity.
      def seconds(millis: Long) = millis / 1000 * 1000
      seconds(path.lastModified) shouldBe seconds(newTime)
    }
  }

  it should "not create intermediate directories" in {
    withTemporaryDirectory("GrizzledFile") { dir =>
      val absDir = dir.getAbsolutePath
      val f = new File(fileutil.joinPath(absDir, "foo", "bar", "baz.txt"))
      f.touch() shouldBe 'failure
    }
  }

  "pathExists" should "return Success for an existing file" in {
    withTemporaryDirectory("GrizzedFile") { dir =>
      val path = new File(fileutil.joinPath(dir.getAbsolutePath, "foo.txt"))
      path.touch() shouldBe 'success
      path.pathExists shouldBe 'success
    }
  }

  it should "return Failure for a nonexistent file" in {
    withTemporaryDirectory("GrizzedFile") { dir =>
      val path = new File(fileutil.joinPath(dir.getAbsolutePath, "foo.txt"))
      path.pathExists shouldBe 'failure
    }
  }

  "isEmpty" should "return true for an empty directory" in {
    withTemporaryDirectory("GrizzledFile") { dir =>
      dir.isEmpty shouldBe true
    }
  }

  it should "return false for a non-empty directory" in {
    withTemporaryDirectory("GrizzledFile") { dir =>
      val f = new File(fileutil.joinPath(dir.getAbsolutePath, "foo.txt"))
      f.touch() shouldBe 'success
      dir.isEmpty shouldBe false
    }
  }

  it should "fail with an assertion error for a non-directory" in {
    withTemporaryDirectory("GrizzledFile") { dir =>
      val f = new File(fileutil.joinPath(dir.getAbsolutePath, "foo.txt"))
      f.touch() shouldBe 'success

      an [AssertionError] should be thrownBy {
        f.isEmpty
      }
    }
  }

  "deleteRecursively" should "delete an entire tree" in {
    withTemporaryDirectory("GrizzledFile") { dir =>
      val absDir = dir.getAbsolutePath
      val topDir = new File(fileutil.joinPath(absDir, "foo"))
      val parentDir = new File(fileutil.joinPath(topDir.getPath, "bar", "baz"))
      val file = new File(fileutil.joinPath(parentDir.getPath, "quux.txt"))

      parentDir.mkdirs() should be (true)
      parentDir should exist
      file.touch() shouldBe 'success

      val t = topDir.deleteRecursively()
      t shouldBe 'success
      t.get shouldEqual 1
      topDir should not (exist)
      new File(absDir) should exist
    }
  }
}
