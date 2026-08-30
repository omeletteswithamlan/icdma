import Link from 'next/link';

export const metadata = { title: 'About — iCDMA' };

const people = [
  {
    name: 'Amlan Mukherjee',
    role: 'Creator. Built the Virtual Coach as his doctoral work at the University of Washington, then led the iCDMA research at Michigan Tech as principal investigator, and taught construction engineering with these ideas for twenty years.',
    href: 'https://www.linkedin.com/in/amlan-mukherjee-ph-d-p-e-73bb0315/',
    linkLabel: 'LinkedIn',
  },
  {
    name: 'Eddy M. Rojas',
    role: 'Doctoral advisor and co-author of the Virtual Coach research at the University of Washington (2003–2006), where situational simulation for construction education took shape.',
    href: 'https://www.linkedin.com/in/eddy-rojas-5168ab24/',
    linkLabel: 'LinkedIn',
  },
  {
    name: 'William D. Winn',
    role: 'Learning scientist at the University of Washington whose work on cognition in interactive environments grounded the pedagogy; co-author of the 2005 learning study. Remembered with gratitude.',
  },
  {
    name: 'Nilufer Onder',
    role: 'Computer scientist at Michigan Tech (AI planning and decision-making under uncertainty); co-author of the iCDMA research from 2009 to 2013, where the temporal-network engine took its formal shape.',
    href: 'https://www.mtu.edu/cs/department/people/faculty/n-onder/',
    linkLabel: 'Michigan Tech',
  },
  {
    name: 'G. Ryan Anderson',
    role: 'Co-author of the 2009 paper introducing the TONAE representation, and a principal developer of the original Java engine.',
    href: 'https://www.linkedin.com/in/gryananderson/',
    linkLabel: 'LinkedIn',
  },
  {
    name: 'Matthew T. Watkins',
    role: 'Graduate researcher at Michigan Tech; co-author of the 2009 studies using adaptive simulations to model decision-making cognition, and agent-based modeling of construction labor productivity.',
  },
  {
    name: 'Pei Tang',
    role: 'Doctoral researcher at Michigan Tech; co-author of the 2010–2013 work using iCDMA to assess contingency-management strategies and activity criticality, validated against a real Michigan DOT highway reconstruction.',
    href: 'https://www.linkedin.com/in/peitang/',
    linkLabel: 'LinkedIn',
  },
  {
    name: 'Darrell Cass',
    role: 'Graduate researcher at Michigan Tech who collected the field data on the I-69 highway reconstruction — the records behind the validation studies you can replay here — and co-authored the project greenhouse-gas accounting work.',
    href: 'https://www.linkedin.com/in/darrellcass/',
    linkLabel: 'LinkedIn',
  },
];

const papers: { text: string; href?: string }[] = [
  { text: 'Rojas, E. M., and Mukherjee, A. (2003). "Modeling the Construction Management Process to Support Situational Simulations." Journal of Computing in Civil Engineering, 17(4), 273–280.', href: 'https://doi.org/10.1061/(ASCE)0887-3801(2003)17:4(273)' },
  { text: 'Rojas, E. M., and Mukherjee, A. (2005). "Interval Temporal Logic in General-Purpose Situational Simulations." Journal of Computing in Civil Engineering, 19(1).', href: 'https://doi.org/10.1061/(ASCE)0887-3801(2005)19:1(83)' },
  { text: 'Rojas, E. M., and Mukherjee, A. (2005). "General-Purpose Situational Simulation Environment for Construction Education." Journal of Construction Engineering and Management, 131(3).', href: 'https://doi.org/10.1061/(ASCE)0733-9364(2005)131:3(319)' },
  { text: 'Mukherjee, A., Winn, W. D., and Rojas, E. M. (2005). "Using Agent Driven Situational Simulations for Training Construction Managers." American Educational Research Association Annual Meeting.' },
  { text: 'Mukherjee, A. (2005). "A Multi-Agent Framework for General Purpose Situational Simulations in Construction Management." Doctoral dissertation, University of Washington.', href: 'https://pages.mtu.edu/~amlan/thesis_control.pdf' },
  { text: 'Rojas, E. M., and Mukherjee, A. (2006). "Multi-Agent Framework for General-Purpose Situational Simulations in Construction Management." Journal of Computing in Civil Engineering, 20(3).', href: 'https://doi.org/10.1061/(ASCE)0887-3801(2006)20:3(165)' },
  {
    text: 'Watkins, M. T., and Mukherjee, A. (2009). "Using Adaptive Simulations to Develop Cognitive Situational Models of Human Decision-making." Technology, Instruction, Cognition and Learning, 6(3), 177–192.',
    href: 'https://www.oldcitypublishing.com/journals/ticl-home/ticl-issue-contents/ticl-volume-6-number-3-2009/ticl-6-3-p-177-192/',
  },
  {
    text: 'Watkins, M. T., Mukherjee, A., Onder, N., and Mattila, K. G. (2009). "Using Agent Based Modeling to Study Construction Labor Productivity as an Emergent Property of Individual and Crew Interactions." Journal of Construction Engineering and Management, 135(7), 657.',
    href: 'https://doi.org/10.1061/(ASCE)CO.1943-7862.0000022',
  },
  { text: 'Anderson, G. R., Mukherjee, A., and Onder, N. (2009). "Traversing and querying constraint driven temporal networks to estimate construction contingencies." Automation in Construction, 18(6), 798–813.', href: 'https://doi.org/10.1016/j.autcon.2009.03.010' },
  { text: 'Onder, N., Mukherjee, A., and Tang, P. (2010). "Construction Management Applications: Challenges in Developing Execution Control Plans." Proceedings of the Twentieth International Conference on Automated Planning and Scheduling (ICAPS).', href: 'https://aaai.org/papers/00263-13397-construction-management-applications-challenges-in-developing-execution-control-plans/' },
  { text: 'Tang, P., Mukherjee, A., and Onder, N. (2013). "Using an interactive schedule simulation platform to assess and improve contingency management strategies." Automation in Construction, 35, 551–560.', href: 'https://doi.org/10.1016/j.autcon.2013.07.005' },
  { text: 'Tang, P., Mukherjee, A., and Onder, N. (2013). "Construction Schedule Simulation for Improved Project Planning: Activity Criticality Index Assessment." Proceedings of the 2013 Winter Simulation Conference.', href: 'https://www.semanticscholar.org/paper/2108419f89934cfa393262cd457efcdf9e0f63c0' },
];

export default function AboutPage() {
  return (
    <main style={{ maxWidth: '44rem', margin: '0 auto', padding: '2.5rem 1.2rem 4rem' }}>
      <div className="label">
        <Link href="/" style={{ color: 'inherit', textDecoration: 'none' }}>iCDMA</Link> · About
      </div>
      <h1 style={{ fontSize: '1.7rem', margin: '0.3rem 0 1rem' }}>
        Twenty years of research, back on the job
      </h1>

      <p>
        iCDMA — the Interactive Construction Decision-Making Aid — is a situational
        simulation for construction management: you run a project day by day, deciding
        crews, hours, and material orders while weather, deliveries, and your earlier
        choices push back. It began as the <em>Virtual Coach</em>, Amlan
        Mukherjee&apos;s doctoral research at the University of Washington (2000–2005),
        and grew at Michigan Technological University (2006–2013) into a
        temporal-constraint-network engine used to study how construction managers
        develop judgment. The original Java system and its scenario database were
        restored in 2026, the engine was rebuilt for the web and verified against the
        original&apos;s behavior, and the scenarios you can play here are the research
        scenarios — including the I-69 highway reconstruction used to validate the
        engine against a real project&apos;s records.
      </p>

      <h2 style={{ fontSize: '1.15rem', margin: '2rem 0 0.6rem' }}>People</h2>
      <div style={{ display: 'flex', flexDirection: 'column', gap: '0.9rem' }}>
        {people.map((p) => (
          <div key={p.name}>
            <strong style={{ fontFamily: 'var(--font-display)' }}>{p.name}</strong>
            {p.href && (
              <>
                {' '}·{' '}
                <a href={p.href} target="_blank" rel="noopener noreferrer">{p.linkLabel}</a>
              </>
            )}
            <div style={{ fontSize: '0.92rem', color: 'var(--muted)' }}>{p.role}</div>
          </div>
        ))}
      </div>
      <p style={{ fontSize: '0.92rem', color: 'var(--muted)' }}>
        The original Java implementation also carries the work of student developers
        over the years, among them{' '}
        <a href="https://www.linkedin.com/in/coreytebo/" target="_blank" rel="noopener noreferrer">Corey Tebo</a>,
        whose 2009 refactoring notes guided parts of the modern rebuild.
      </p>

      <h2 style={{ fontSize: '1.15rem', margin: '2rem 0 0.6rem' }}>Publications</h2>
      <ol style={{ paddingLeft: '1.2rem', display: 'flex', flexDirection: 'column', gap: '0.5rem', fontSize: '0.92rem' }}>
        {papers.map((p) => (
          <li key={p.text}>
            {p.text}
            {p.href && <>{' '}<a href={p.href} target="_blank" rel="noopener noreferrer">link</a></>}
          </li>
        ))}
      </ol>

      <h2 style={{ fontSize: '1.15rem', margin: '2rem 0 0.6rem' }}>Funding</h2>
      <p style={{ fontSize: '0.95rem' }}>
        The iCDMA research was supported by the National Science Foundation under
        award{' '}
        <a href="https://www.nsf.gov/awardsearch/show-award?AWD_ID=0624118" target="_blank" rel="noopener noreferrer">
          CMMI-0624118
        </a>
        , &ldquo;DRU Collaborative Research: Understanding mental models of expertise in
        construction management using interactive adaptive simulations&rdquo;
        (2006–2011). Any opinions, findings, and conclusions expressed in this
        material are those of the authors and do not necessarily reflect the views of
        the National Science Foundation.
      </p>

      <h2 style={{ fontSize: '1.15rem', margin: '2rem 0 0.6rem' }}>Source</h2>
      <p style={{ fontSize: '0.95rem' }}>
        The restored 2000–2013 Java system, the verified TypeScript engine, the
        recovered scenarios, and this application are at{' '}
        <a href="https://github.com/omeletteswithamlan/icdma" target="_blank" rel="noopener noreferrer">
          github.com/omeletteswithamlan/icdma
        </a>.
      </p>
    </main>
  );
}
