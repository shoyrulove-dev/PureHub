# IEEE OUI data

PureHub's offline vendor lookup is generated from the IEEE Registration Authority public MA-L, MA-M and MA-S CSV listings.

- Source: https://standards.ieee.org/products-programs/regauth/
- Generator: `tools/generate-ieee-oui.mjs`
- Matching: longest prefix first (MA-S, MA-M, MA-L)
- Privacy: lookup is fully offline; BSSIDs are not uploaded.

IEEE and the listed organization names remain the property of their respective owners. Inclusion does not imply endorsement. Before redistributing a release, maintainers must re-check the IEEE site's current public-listing terms and attribution requirements.
