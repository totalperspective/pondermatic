declare module '@totalperspective/pondermatic' {
  interface Pool {
    id: string;
  }
  
  interface Local {
    engine: unknown;
  }

  export type Engine = Pool | Local;
  
  export type Datom = [string, string, unknown, number, boolean];
  export type Op = 'db/add' | 'db/retract';
  export type TxDatum = [Op, string, string, unknown]
  export type TxData<T extends object> = (TxDatum | Entity<T>)[];

  export interface DBResult {
    'query': (query: string) => any;
    'tx-data': Datom[];
    'tempids': Record<string, string>;
    'db-uri': string;
  }

  type InsertOp = '->db'
  type UpsertOp = '+>db'
  type DatomsOp = '!>db'
  type RetractOp = '-!>db'
  export type TxOp = InsertOp | UpsertOp | DatomsOp | RetractOp
  export type TxCallback = (result: DBResult) => void
  export type Entity<T extends object> = {
    id: string;
    [key in Exclude<keyof T, 'id'>]: T[key];
  }

  export interface TxMessage<T extends object> {
    '->db': TxData<T>;
    cb?: TxCallback;
  }

  export interface UpsertMessage<T extends object> {
    '+>db': TxData<T>;
    cb?: TxCallback;
  }

  export interface RetractMessage<T extends object> {
    '-!>db': TxData<T>;
    cb?: TxCallback;
  }

  export interface DatomsMessage {
    '!>db': Datom[];
    cb?: TxCallback;
  }

  type Messages<T extends object> = {
    'insert': TxMessage<T>;
    'upsert': UpsertMessage<T>;
    'retract': RetractMessage<T>;
    'datoms': DatomsMessage;
  }

  
  export type TxType = keyof Messages<object>
  export type Message<T extends TxType = 'insert', D extends object = object> = Messages<D>[T]

  export type Task = { __type: 'flow' } & (() => void)
  export type State = { 'quiescent?': boolean, 'prefix': string } & { [key: string]: any }

  export interface Rule {
    'id': string
    'rule/when': WhenClause
    'rule/then': ThenClause
  }

  export type RuleSet = Entity<Rule>[] & { __type: 'ruleset' }

  interface PondermaticAPI {
    createEngine(name: string, resetDb?: boolean): Engine;
    copy(engine: Engine): Engine;
    ruleset(rules: Rule[]): RuleSet;
    dataset<A extends object, T extends A[] = A[]>(data: T): TxData<A>;
    sh<T extends TxType = 'insert', D extends object = object>(engine: Engine, msg: Message<T, D>): Promise<State>;
    cmd(msg: object): void;
    addRulesMsg(rules: object): object;
    q(engine: Engine, query: string, args: any[], callback: (result: any) => void): Task;
    qP(engine: Engine, query: string, args: any[]): Promise<any>;
    q$(engine: Engine, query: string, ...args: any[]): any;
    entity(engine: Engine, ident: string | object, callback: (entity: any) => void): Task;
    entityP(engine: Engine, ident: string | object): Promise<any>;
    basisT(engine: Engine, callback: (result: number) => void): Task;
    watchEntity(engine: Engine, ident: string | object, callback: (entity: any) => void): Task;
    hashId(obj: object): string;
    errorInfo(error: Error): object;
    portal(launcher?: string): void;
    dispose(task: Task): void;
    log(level: string | null, expr: any): void;
    unify(exprOrStr: string | object, env: object | object[]): object;
    pprint(obj: any): void;
    addTap(tap?: (x: any) => void): void;
    readString(str: string): any;
    toString(obj: any): string;
    encode(obj: any): string;
    decode(str: string): any;
    eval(str: string, toJs?: boolean, opts?: object): any;
    toJS(form: any): any;
    devtoolsFormatter: {
      header: (obj: any, config: any) => any;
      hasBody: () => boolean;
      body: (obj: any, config: any) => any;
    };
    stop(engine: Engine): void;
    watchAgents(callback: (agents: any) => void): string;
    removeAgentsWatch(id: string): void;
    isReady(engine: Engine): boolean;
    devtoolsInit(): void;
    noop: {
      rules: Message;
      engine: Message;
      db: Message;
    };
  }

  const api: PondermaticAPI;
  export default api;
}
