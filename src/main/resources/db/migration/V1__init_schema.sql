-- =====================================================
-- V1__init_schema.sql
-- 현재 프로덕션 스키마 초기화 (Flyway baseline)
-- 기존 환경: baseline-on-migrate=true, baseline-version=1 로 이 파일 건너뜀
-- 신규 환경: 이 파일로 전체 스키마 생성
-- 제외된 레거시 테이블: member_recommendation_problem, team_problem,
--                       team_recommendation, team_recommendation_problem
-- =====================================================

-- 1. tag (마스터 테이블, 의존성 없음)
CREATE TABLE tag
(
    tag_key varchar(64)  NOT NULL COMMENT 'solved.ac 태그 키 (dp, greedy 등)'
        PRIMARY KEY,
    name_ko varchar(100) NOT NULL COMMENT '한글 태그명',
    name_en varchar(100) NOT NULL COMMENT '영문 태그명'
) COLLATE = utf8mb4_unicode_ci;

INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('math', '수학', 'mathematics');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('implementation', '구현', 'implementation');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('dp', '다이나믹 프로그래밍', 'dynamic programming');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('data_structures', '자료 구조', 'data structures');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('graphs', '그래프 이론', 'graph theory');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('greedy', '그리디 알고리즘', 'greedy');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('string', '문자열', 'string');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('bruteforcing', '브루트포스 알고리즘', 'bruteforcing');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('graph_traversal', '그래프 탐색', 'graph traversal');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('sorting', '정렬', 'sorting');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('ad_hoc', '애드 혹', 'ad-hoc');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('geometry', '기하학', 'geometry');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('trees', '트리', 'tree');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('number_theory', '정수론', 'number theory');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('segtree', '세그먼트 트리', 'segment tree');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('binary_search', '이분 탐색', 'binary search');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('set', '집합과 맵', 'set / map');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('constructive', '해 구성하기', 'constructive');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('arithmetic', '사칙연산', 'arithmetic');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('simulation', '시뮬레이션', 'simulation');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('prefix_sum', '누적 합', 'prefix sum');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('combinatorics', '조합론', 'combinatorics');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('bfs', '너비 우선 탐색', 'breadth-first search');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('case_work', '많은 조건 분기', 'case work');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('bitmask', '비트마스킹', 'bitmask');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('shortest_path', '최단 경로', 'shortest path');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('dfs', '깊이 우선 탐색', 'depth-first search');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('hash_set', '해시를 사용한 집합과 맵', 'set / map by hashing');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('dijkstra', '데이크스트라', 'dijkstra''s');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('sweeping', '스위핑', 'sweeping');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('disjoint_set', '분리 집합', 'disjoint set');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('backtracking', '백트래킹', 'backtracking');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('dp_tree', '트리에서의 다이나믹 프로그래밍', 'dynamic programming on trees');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('priority_queue', '우선순위 큐', 'priority queue');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('tree_set', '트리를 사용한 집합과 맵', 'set / map by trees');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('parsing', '파싱', 'parsing');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('game_theory', '게임 이론', 'game theory');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('parametric_search', '매개 변수 탐색', 'parametric search');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('divide_and_conquer', '분할 정복', 'divide and conquer');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('two_pointer', '두 포인터', 'two-pointer');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('probability', '확률론', 'probability theory');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('stack', '스택', 'stack');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('lazyprop', '느리게 갱신되는 세그먼트 트리', 'segment tree with lazy propagation');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('primality_test', '소수 판정', 'primality test');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('flow', '최대 유량', 'maximum flow');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('dp_bitfield', '비트필드를 이용한 다이나믹 프로그래밍', 'dynamic programming using bitfield');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('offline_queries', '오프라인 쿼리', 'offline queries');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('exponentiation_by_squaring', '분할 정복을 이용한 거듭제곱', 'exponentiation by squaring');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('knapsack', '배낭 문제', 'knapsack');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('dag', '방향 비순환 그래프', 'directed acyclic graph');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('coordinate_compression', '값 / 좌표 압축', 'value / coordinate compression');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('recursion', '재귀', 'recursion');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('arbitrary_precision', '임의 정밀도 / 큰 수 연산', 'arbitrary precision / big integers');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('euclidean', '유클리드 호제법', 'euclidean algorithm');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('mst', '최소 스패닝 트리', 'minimum spanning tree');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('precomputation', '런타임 전의 전처리', 'precomputation');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('topological_sorting', '위상 정렬', 'topological sorting');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('convex_hull', '볼록 껍질', 'convex hull');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('linear_algebra', '선형대수학', 'linear algebra');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('sieve', '에라토스테네스의 체', 'sieve of eratosthenes');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('grid_graph', '격자 그래프', 'grid graph');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('bipartite_matching', '이분 매칭', 'bipartite matching');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('inclusion_and_exclusion', '포함 배제의 원리', 'inclusion and exclusion');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('lca', '최소 공통 조상', 'lowest common ancestor');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('sparse_table', '희소 배열', 'sparse table');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('randomization', '무작위화', 'randomization');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('hashing', '해싱', 'hashing');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('floyd_warshall', '플로이드–워셜', 'floyd–warshall');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('modular_multiplicative_inverse', '모듈로 곱셈 역원', 'modular multiplicative inverse');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('scc', '강한 연결 요소', 'strongly connected component');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('parity', '홀짝성', 'parity');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('traceback', '역추적', 'traceback');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('smaller_to_larger', '작은 집합에서 큰 집합으로 합치는 테크닉', 'smaller to larger technique');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('line_intersection', '선분 교차 판정', 'line segment intersection check');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('fft', '고속 푸리에 변환', 'fast fourier transform');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('calculus', '미적분학', 'calculus');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('sqrt_decomposition', '제곱근 분할법', 'square root decomposition');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('trie', '트라이', 'trie');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('deque', '덱', 'deque');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('geometry_3d', '3차원 기하학', 'geometry; 3d');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('ternary_search', '삼분 탐색', 'ternary search');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('heuristics', '휴리스틱', 'heuristics');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('euler_tour_technique', '오일러 경로 테크닉', 'euler tour technique');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('suffix_array', '접미사 배열과 LCP 배열', 'suffix array and lcp array');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('mcmf', '최소 비용 최대 유량', 'minimum cost maximum flow');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('sprague_grundy', '스프라그–그런디 정리', 'sprague–grundy theorem');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('sliding_window', '슬라이딩 윈도우', 'sliding window');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('prime_factorization', '소인수분해', 'prime factorization');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('cht', '볼록 껍질을 이용한 최적화', 'convex hull trick');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('centroid', '센트로이드', 'centroid');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('pythagoras', '피타고라스 정리', 'pythagoras theorem');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('permutation_cycle_decomposition', '순열 사이클 분할', 'permutation cycle decomposition');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('mitm', '중간에서 만나기', 'meet in the middle');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('lis', '가장 긴 증가하는 부분 수열 문제', 'longest increasing sequence problem');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('bitset', '비트 집합', 'bit set');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('kmp', 'KMP', 'knuth–morris–pratt');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('gaussian_elimination', '가우스 소거법', 'gaussian elimination');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('hld', 'Heavy-light 분할', 'heavy-light decomposition');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('linearity_of_expectation', '기댓값의 선형성', 'linearity of expectation');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('bipartite_graph', '이분 그래프', 'bipartite graph');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('difference_array', '차분 배열 트릭', 'difference array');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('polygon_area', '다각형의 넓이', 'area of a polygon');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('mfmc', '최대 유량 최소 컷 정리', 'max-flow min-cut theorem');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('centroid_decomposition', '센트로이드 분할', 'centroid decomposition');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('physics', '물리학', 'physics');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('flt', '페르마의 소정리', 'fermat''s little theorem');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('0_1_bfs', '0-1 너비 우선 탐색', '0-1 bfs');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('eulerian_path', '오일러 경로', 'eulerian path / circuit');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('flood_fill', '플러드 필', 'flood-fill');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('articulation', '단절점과 단절선', 'articulation points and bridges');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('2_sat', '2-sat', '2-sat');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('queue', '큐', 'queue');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('functional_graph', '함수형 그래프', 'functional graph');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('tsp', '외판원 순회 문제', 'travelling salesman problem');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('pigeonhole_principle', '비둘기집 원리', 'pigeonhole principle');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('deque_trick', '덱을 이용한 구간 최댓값 트릭', 'deque range maximum trick');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('rerooting', '트리에서의 전방향 다이나믹 프로그래밍', 'rerooting on trees');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('planar_graph', '평면 그래프', 'planar graph');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('generating_function', '생성 함수', 'generating function');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('pst', '퍼시스턴트 세그먼트 트리', 'persistent segment tree');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('euler_phi', '오일러 피 함수', 'euler totient function');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('point_in_convex_polygon', '볼록 다각형 내부의 점 판정', 'point in convex polygon check');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('bcc', '이중 연결 요소', 'biconnected component');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('crt', '중국인의 나머지 정리', 'chinese remainder theorem');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('dp_digit', '자릿수를 이용한 다이나믹 프로그래밍', 'digit dp');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('linked_list', '연결 리스트', 'linked list');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('angle_sorting', '각도 정렬', 'angle sorting');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('harmonic_number', '조화수', 'harmonic number');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('mo', 'mo''s', 'mo''s');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('tree_diameter', '트리의 지름', 'diameter of a tree');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('cactus', '선인장', 'cactus');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('invariant', '불변량 찾기', 'finding invariants');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('bellman_ford', '벨만–포드', 'bellman–ford');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('extended_euclidean', '확장 유클리드 호제법', 'extended euclidean algorithm');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('splay_tree', '스플레이 트리', 'splay tree');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('maximum_subarray', '최대 부분 배열 문제', 'maximum subarray');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('divide_and_conquer_optimization', '분할 정복을 사용한 최적화', 'divide and conquer optimization');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('pbs', '병렬 이분 탐색', 'parallel binary search');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('dp_sum_over_subsets', '부분집합의 합 다이나믹 프로그래밍', 'sum over subsets dynamic programming');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('half_plane_intersection', '반평면 교집합', 'half plane intersection');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('rotating_calipers', '회전하는 캘리퍼스', 'rotating calipers');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('euler_characteristic', '오일러 지표 (χ=V-E+F)', 'euler characteristic (χ=v-e+f)');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('multi_segtree', '다차원 세그먼트 트리', 'multidimensional segment tree');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('regex', '정규 표현식', 'regular expression');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('slope_trick', '함수 개형을 이용한 최적화', 'slope trick');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('dp_deque', '덱을 이용한 다이나믹 프로그래밍', 'dynamic programming using a deque');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('manacher', '매내처', 'manacher''s');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('pollard_rho', '폴라드 로', 'pollard rho');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('miller_rabin', '밀러–라빈 소수 판별법', 'miller–rabin');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('aho_corasick', '아호-코라식', 'aho-corasick');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('lcs', '최장 공통 부분 수열 문제', 'longest common subsequence');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('mobius_inversion', '뫼비우스 반전 공식', 'möbius inversion');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('tree_isomorphism', '트리 동형 사상', 'tree isomorphism');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('merge_sort_tree', '머지 소트 트리', 'merge sort tree');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('link_cut_tree', '링크/컷 트리', 'link/cut tree');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('cartesian_tree', '데카르트 트리', 'cartesian tree');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('numerical_analysis', '수치해석', 'numerical analysis');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('dp_connection_profile', '커넥션 프로파일을 이용한 다이나믹 프로그래밍', 'dynamic programming using connection profile');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('point_in_non_convex_polygon', '오목 다각형 내부의 점 판정', 'point in non-convex polygon check');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('rabin_karp', '라빈–카프', 'rabin–karp');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('simulated_annealing', '담금질 기법', 'simulated annealing');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('hall', '홀의 결혼 정리', 'hall''s theorem');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('z', 'z', 'z');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('statistics', '통계학', 'statistics');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('berlekamp_massey', '벌리캠프–매시', 'berlekamp–massey');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('offline_dynamic_connectivity', '오프라인 동적 연결성 판정', 'offline dynamic connectivity');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('geometric_boolean_operations', '도형에서의 불 연산', 'boolean operations on geometric objects');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('xor_basis', '배타적 논리합 기저 (gf(2))', 'xor basis (gf(2))');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('li_chao_tree', '리–차오 트리', 'li–chao tree');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('alien', 'Aliens 트릭', 'aliens trick');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('hungarian', '헝가리안', 'hungarian');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('tree_compression', '트리 압축', 'tree compression');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('linear_programming', '선형 계획법', 'linear programming');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('beats', '세그먼트 트리 비츠', 'segment tree beats');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('lucas', '뤼카 정리', 'lucas theorem');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('duality', '쌍대성', 'duality');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('voronoi', '보로노이 다이어그램', 'voronoi diagram');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('circulation', '서큘레이션', 'circulation');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('green', '그린 정리', 'green''s theorem');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('dual_graph', '쌍대 그래프', 'dual graph');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('polynomial_interpolation', '다항식 보간법', 'polynomial interpolation');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('general_matching', '일반적인 매칭', 'general matching');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('monotone_queue_optimization', '단조 큐를 이용한 최적화', 'monotone queue optimization');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('cdq', 'cdq 분할 정복', 'cdq');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('pick', '픽의 정리', 'pick''s theorem');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('matroid', '매트로이드', 'matroid');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('kitamasa', '다항식을 이용한 선형점화식 계산', 'linear recurrence calculation by polynomials');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('suffix_tree', '접미사 트리', 'suffix tree');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('discrete_log', '이산 로그', 'discrete logarithm');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('geometry_hyper', '4차원 이상의 기하학', 'geometry; hyperdimensional');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('tree_decomposition', '트리 분할', 'tree decomposition');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('min_enclosing_circle', '최소 외접원', 'minimum enclosing circle');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('differential_cryptanalysis', '차분 공격', 'differential cryptanalysis');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('utf8', 'utf-8 입력 처리', 'utf-8 inputs');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('burnside', '번사이드 보조정리', 'burnside''s lemma');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('bulldozer', 'bulldozer 트릭', 'bulldozer trick');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('degree_sequence', '차수열', 'degree sequence');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('top_tree', '탑 트리', 'top tree');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('dominator_tree', '도미네이터 트리', 'dominator tree');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('bidirectional_search', '양방향 탐색', 'bidirectional search');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('lgv', '린드스트롬–게셀–비엔노 보조정리', 'lindström–gessel–viennot lemma');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('knuth_x', '크누스 X', 'knuth''s x');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('dancing_links', '춤추는 링크', 'dancing links');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('discrete_sqrt', '이산 제곱근', 'discrete square root');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('palindrome_tree', '회문 트리', 'palindrome tree');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('gradient_descent', '경사 하강법', 'gradient descent');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('rope', '로프', 'rope');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('pisano', '피사노 주기', 'pisano period');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('floor_sum', '유리 등차수열의 내림 합', 'sum of floor of rational arithmetic sequence');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('stable_marriage', '안정 결혼 문제', 'stable marriage problem');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('bayes', '베이즈 정리', 'bayes theorem');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('delaunay', '델로네 삼각분할', 'delaunay triangulation');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('knuth', '크누스 최적화', 'knuth optimization');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('bitset_lcs', '비트 집합을 이용한 최장 공통 부분 수열 최적화', 'longest common subsequence using bit sets');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('kinetic_segtree', '키네틱 세그먼트 트리', 'kinetic segment tree');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('multipoint_evaluation', '다중 대입값 계산', 'multipoint evaluation');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('dial', '다이얼', 'dial');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('birthday', '생일 문제', 'birthday problem');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('hirschberg', '히르쉬버그', 'hirschberg''s');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('chordal_graph', '현 그래프', 'chordal graph');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('lte', '지수승강 보조정리', 'lifting the exponent lemma');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('directed_mst', '유향 최소 스패닝 트리', 'directed minimum spanning tree');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('stoer_wagner', '스토어–바그너', 'stoer–wagner');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('hackenbush', '하켄부시 게임', 'hackenbush');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('majority_vote', '보이어–무어 다수결 투표', 'boyer–moore majority vote');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('rb_tree', '레드-블랙 트리', 'red-black tree');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('a_star', 'A*', 'a*');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('discrete_kth_root', '이산 k제곱근', 'discrete k-th root');
INSERT INTO tag (tag_key, name_ko, name_en) VALUES ('treewidth', '제한된 트리 너비', 'treewidth');

-- 2. problem (id = 백준 문제 번호, AUTO_INCREMENT 없음)
CREATE TABLE problem
(
    id                  bigint       NOT NULL PRIMARY KEY,
    accepted_user_count int          NULL,
    level               int          NULL,
    title               varchar(255) NOT NULL,
    title_ko            varchar(255) NULL,
    average_tries       double       NULL
);

-- 3. problem_tag (problem-tag N:M, FK 없음 — 앱 레벨 관리)
CREATE TABLE problem_tag
(
    id         bigint      NOT NULL AUTO_INCREMENT PRIMARY KEY,
    problem_id bigint      NOT NULL COMMENT '문제 ID (백준 문제번호)',
    tag_key    varchar(64) NOT NULL COMMENT 'Tag 참조',
    CONSTRAINT uk_problem_tag UNIQUE (problem_id, tag_key)
) COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_problem_tag_problem ON problem_tag (problem_id);
CREATE INDEX idx_problem_tag_tag ON problem_tag (tag_key);

-- 4. member
CREATE TABLE member
(
    id            bigint                           NOT NULL AUTO_INCREMENT PRIMARY KEY,
    created_at    timestamp                        NULL,
    deleted_at    datetime(6)                      NULL,
    modified_at   timestamp                        NULL,
    email         varchar(255)                     NOT NULL,
    handle        varchar(255)                     NULL,
    is_verified   bit                              NOT NULL,
    provider      varchar(255)                     NULL,
    provider_id   varchar(255)                     NOT NULL,
    role          enum ('ROLE_USER', 'ROLE_ADMIN') NULL,
    last_login_at datetime                         NULL,
    CONSTRAINT UK_mbmcqelty0fbrvxp1q58dn57t UNIQUE (email),
    CONSTRAINT UK_q4jvd8lnevoqq74bkjcm3p6ub UNIQUE (provider_id)
);

-- 5. member_solved_problem (FK 제거 — 앱 레벨 관리)
CREATE TABLE member_solved_problem
(
    id          bigint      NOT NULL AUTO_INCREMENT PRIMARY KEY,
    created_at  timestamp   NULL,
    deleted_at  datetime(6) NULL,
    modified_at timestamp   NULL,
    solved_at   datetime(6) NULL,
    member_id   bigint      NOT NULL,
    problem_id  bigint      NOT NULL
);

-- 6. team
CREATE TABLE team
(
    id                        bigint                                     NOT NULL AUTO_INCREMENT PRIMARY KEY,
    created_at                timestamp                                  NULL,
    deleted_at                datetime(6)                                NULL,
    modified_at               timestamp                                  NULL,
    description               varchar(255)                               NULL,
    max_problem_level         int                                        NULL,
    min_problem_level         int                                        NULL,
    name                      varchar(255)                               NOT NULL,
    problem_difficulty_preset enum ('EASY', 'NORMAL', 'HARD', 'CUSTOM') NULL,
    recommendation_days       int                                        NOT NULL,
    recommendation_status     enum ('ACTIVE', 'INACTIVE')                NOT NULL,
    is_private                tinyint(1) DEFAULT 0                       NOT NULL,
    problem_count             int        DEFAULT 3                       NOT NULL
);

-- 7. team_member (FK 제거 — 앱 레벨 관리)
CREATE TABLE team_member
(
    id          bigint                    NOT NULL AUTO_INCREMENT PRIMARY KEY,
    created_at  timestamp                 NULL,
    deleted_at  datetime(6)               NULL,
    modified_at timestamp                 NULL,
    role        enum ('LEADER', 'MEMBER') NOT NULL,
    member_id   bigint                    NOT NULL,
    team_id     bigint                    NOT NULL
);

-- 8. team_include_tag (FK 없음 — 앱 레벨 관리)
CREATE TABLE team_include_tag
(
    id      bigint      NOT NULL AUTO_INCREMENT PRIMARY KEY,
    team_id bigint      NOT NULL COMMENT 'Team 참조',
    tag_key varchar(64) NOT NULL COMMENT 'Tag 참조',
    CONSTRAINT uk_team_include_tag UNIQUE (team_id, tag_key)
) COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_team_include_tag_tag ON team_include_tag (tag_key);
CREATE INDEX idx_team_include_tag_team ON team_include_tag (team_id);

-- 9. team_join
CREATE TABLE team_join
(
    id               bigint      NOT NULL AUTO_INCREMENT PRIMARY KEY,
    team_id          bigint      NOT NULL,
    type             varchar(16) NOT NULL,
    requester_id     bigint      NOT NULL,
    target_member_id bigint      NULL,
    status           varchar(16) DEFAULT 'PENDING' NOT NULL,
    processed_at     datetime(6) NULL,
    expires_at       datetime(6) NOT NULL,
    created_at       datetime(6) NOT NULL,
    modified_at      datetime(6) NOT NULL,
    deleted_at       datetime(6) NULL
);

CREATE INDEX idx_requester_status ON team_join (requester_id, status);
CREATE INDEX idx_target_status ON team_join (target_member_id, status);
CREATE INDEX idx_team_status ON team_join (team_id, status);

-- 10. recommendation (team_id는 스냅샷, FK 없음)
CREATE TABLE recommendation
(
    id          bigint                             NOT NULL AUTO_INCREMENT PRIMARY KEY,
    type        enum ('SCHEDULED', 'MANUAL')       NOT NULL,
    team_id     bigint                             NULL,
    created_at  timestamp DEFAULT CURRENT_TIMESTAMP NULL,
    modified_at timestamp DEFAULT CURRENT_TIMESTAMP NULL ON UPDATE CURRENT_TIMESTAMP,
    deleted_at  timestamp                          NULL
) COLLATE = utf8mb4_unicode_ci;

-- 11. recommendation_problem
CREATE TABLE recommendation_problem
(
    id                bigint    NOT NULL AUTO_INCREMENT PRIMARY KEY,
    recommendation_id bigint    NOT NULL,
    problem_id        bigint    NOT NULL,
    created_at        timestamp DEFAULT CURRENT_TIMESTAMP NULL,
    modified_at       timestamp DEFAULT CURRENT_TIMESTAMP NULL ON UPDATE CURRENT_TIMESTAMP,
    deleted_at        timestamp NULL
) COLLATE = utf8mb4_unicode_ci;

-- 12. member_recommendation (team_id/team_name은 스냅샷, FK 없음)
CREATE TABLE member_recommendation
(
    id                bigint                                             NOT NULL AUTO_INCREMENT PRIMARY KEY,
    member_id         bigint                                             NOT NULL,
    recommendation_id bigint                                             NOT NULL,
    email_send_status enum ('PENDING', 'SENT', 'FAILED') DEFAULT 'PENDING' NOT NULL,
    email_sent_at     timestamp                                          NULL,
    created_at        timestamp DEFAULT CURRENT_TIMESTAMP                NULL,
    modified_at       timestamp DEFAULT CURRENT_TIMESTAMP                NULL ON UPDATE CURRENT_TIMESTAMP,
    deleted_at        timestamp                                          NULL,
    team_id           bigint                                             NULL,
    team_name         varchar(100)                                       NULL,
    CONSTRAINT uk_member_recommendation UNIQUE (member_id, recommendation_id)
) COLLATE = utf8mb4_unicode_ci;

-- 13. notification
CREATE TABLE notification
(
    id           bigint      NOT NULL AUTO_INCREMENT PRIMARY KEY,
    recipient_id bigint      NOT NULL,
    type         varchar(50) NOT NULL,
    metadata     text        NULL,
    read_at      datetime    NULL,
    created_at   datetime    NOT NULL,
    modified_at  datetime    NOT NULL,
    deleted_at   datetime    NULL
);

CREATE INDEX idx_recipient ON notification (recipient_id);

