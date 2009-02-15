#!/bin/sh

here=$(cd $(dirname $0); pwd)
top=$(dirname $(dirname $here))

# Because the Ant scaladoc task isn't working.
scaladoc \
-doctitle "Grizzled Scala Library" \
-windowtitle "Grizzled Scala Library" \
-d $top/docs \
-footer "Copyright 2009 Brian M. Clapper" \
-header "sake: A Scala build tool" \
$(find $top/src -name '*.scala')

