The TreeView implementation in javafx works great for most usecases.
However in my case I needed to be able to bind elements (lines to be specific) to elements in the tree.
The problem however is that I needed to be able to figure out where the elements were (and sometimes they were rebuilt)
You could bind on graphics in the tree but it's not ideal.
I also needed to be able to trigger on visibility changes, not only of the elements themselves but also the parent elements, because the positional binding would point to whatever parent had been closed
After a lot of fiddling and trickery I got something to work with the original treeview but it was ugly and not entirely stable.

This tree is a from scratch alternative implementation of the default tree that has inherent support for the features I needed. 