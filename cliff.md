# Git Cliff

<https://git-cliff.org/docs/>

## Generate Changelog of range of commits

Use the `-c` flag to point to the configuration which `git-cliff` uses.
Use the `-o` flag to set the output file.
Set the commit range with `<commit hash>..<commit hash>`. Here `HEAD` is used as the last commit to have the latest commit messages included.

```bash
git-cliff -c ./cliff.toml -o CHANGELOG.md 1cff1801754686614f0e88ce2c61017fb5541f21..HEAD
```
