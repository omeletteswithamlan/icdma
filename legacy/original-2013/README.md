# Original source, as developed 2000–2013

The iCDMA Java source exactly as archived from the research years (CVS working
copy, last modified ~2010–2011), with a single change: the database password in
`Simulator.java` is masked. **This tree is frozen — do not edit it.**

All modern work happens in the sibling `legacy/src` tree. The complete
difference between this original and the living copy is maintained at
[docs/modernization.patch](../../docs/modernization.patch) and explained in
[docs/restoration-log.md](../../docs/restoration-log.md):

```
diff -ruN legacy/original-2013/src legacy/src
```

The `TODO` file is the original 2009 refactoring plan, preserved as found.
