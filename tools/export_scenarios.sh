#!/bin/zsh
# Export each vcdb project as a self-contained JSON scenario file.
# First-cut format: a faithful per-table export filtered to one project.
# The Phase 1 engine defines the real scenario schema; this preserves content.
set -e
PG=/opt/homebrew/opt/postgresql@14/bin
PSQL() { "$PG/psql" -h /tmp -p 5433 -U postgres -d vcdb -t -A -c "$1"; }
OUT="$(dirname "$0")/../scenarios"
mkdir -p "$OUT"

for pid in $(PSQL "select projectid from project order by projectid"); do
  slug=$(PSQL "select lower(regexp_replace(description,'[^a-zA-Z0-9]+','-','g')) from project where projectid=$pid")
  PSQL "
select json_build_object(
  'format', 'icdma-scenario-raw/0.1',
  'source', 'vcdb (MTU server backup, May 2024)',
  'project', (select row_to_json(p) from project p where projectid=$pid),
  'activities', (select coalesce(json_agg(row_to_json(a) order by a.activityid),'[]'::json)
                 from activity a where projectid=$pid),
  'constraints', (select coalesce(json_agg(row_to_json(c) order by c.constraintid),'[]'::json)
                  from constraints c
                  where c.fromactivityid in (select activityid from activity where projectid=$pid)
                     or c.toactivityid   in (select activityid from activity where projectid=$pid)),
  'materialuse', (select coalesce(json_agg(row_to_json(mu) order by mu.materialuseid),'[]'::json)
                  from materialuse mu join activity a on a.activityid=mu.activityid where a.projectid=$pid),
  'laboruse', (select coalesce(json_agg(row_to_json(lu) order by lu.laboruseid),'[]'::json)
               from laboruse lu join activity a on a.activityid=lu.activityid where a.projectid=$pid),
  'laborcrewuse', (select coalesce(json_agg(row_to_json(lcu) order by lcu.laborcrewuseid),'[]'::json)
                   from laborcrewuse lcu join activity a on a.activityid=lcu.activityid where a.projectid=$pid),
  'driving_materials', (select coalesce(json_agg(row_to_json(dm) order by dm.driving_materialid),'[]'::json)
                        from driving_material dm join activity a on a.activityid=dm.activityid where a.projectid=$pid),
  'materials', (select coalesce(json_agg(row_to_json(m) order by m.materialid),'[]'::json)
                from material m where m.materialid in
                  (select mu.materialid from materialuse mu join activity a on a.activityid=mu.activityid where a.projectid=$pid
                   union select dm.materialid from driving_material dm join activity a on a.activityid=dm.activityid where a.projectid=$pid)),
  'labor', (select coalesce(json_agg(row_to_json(l) order by l.laborid),'[]'::json)
            from labor l where l.laborid in
              (select lu.laborid from laboruse lu join activity a on a.activityid=lu.activityid where a.projectid=$pid
               union select lce.laborid from laborcrewentry lce
                     join laborcrewuse lcu on lcu.laborcrewid=lce.laborcrewid
                     join activity a on a.activityid=lcu.activityid where a.projectid=$pid)),
  'laborcrews', (select coalesce(json_agg(json_build_object(
                    'laborcrewid', lc.laborcrewid,
                    'description', lc.description,
                    'entries', (select coalesce(json_agg(row_to_json(lce) order by lce.laborcrewentryid),'[]'::json)
                                from laborcrewentry lce where lce.laborcrewid=lc.laborcrewid)
                  ) order by lc.laborcrewid),'[]'::json)
                 from laborcrew lc where lc.laborcrewid in
                   (select lcu.laborcrewid from laborcrewuse lcu
                    join activity a on a.activityid=lcu.activityid where a.projectid=$pid)),
  'rules', (select coalesce(json_agg(json_build_object(
              'ruleid', r.ruleid,
              'ordering', pr.ordering,
              'description', r.description,
              'message', r.message,
              'probability', r.probability,
              'global', r.global,
              'preconditions', (select coalesce(json_agg(row_to_json(pc) order by pc.preconditionid),'[]'::json)
                                from ruleprecondition rp join precondition pc on pc.preconditionid=rp.preconditionid
                                where rp.ruleid=r.ruleid),
              'postconditions', (select coalesce(json_agg(row_to_json(poc) order by poc.postconditionid),'[]'::json)
                                 from rulepostcondition rpo join postcondition poc on poc.postconditionid=rpo.postconditionid
                                 where rpo.ruleid=r.ruleid),
              'resources', (select coalesce(json_agg(row_to_json(rr) order by rr.ruleresourceid),'[]'::json)
                            from ruleresource rr where rr.ruleid=r.ruleid)
            ) order by pr.ordering, r.ruleid),'[]'::json)
            from projectrule pr join rule r on r.ruleid=pr.ruleid where pr.projectid=$pid),
  'variables', (select coalesce(json_agg(row_to_json(v) order by v.variableid),'[]'::json) from variable v)
)" | python3 -m json.tool > "$OUT/project-$pid-$slug.json"
  echo "exported project $pid -> project-$pid-$slug.json"
done
